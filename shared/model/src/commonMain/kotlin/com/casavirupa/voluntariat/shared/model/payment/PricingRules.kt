package com.casavirupa.voluntariat.shared.model.payment

import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlinx.datetime.LocalDate

enum class ChargeableItem {
    Lunch,
    Dinner,
    Breakfast,
    BreakfastNextDay,
    Sleep,
}

/**
 * One row of the dashboard's price table (`item_price_rules/dash-<n>`). A null criterion
 * means "any". Contract: `../voluntariat-dashboard/PRICES-APP.md`.
 */
data class ItemPriceRule(
    val id: String,
    val item: ChargeableItem,
    val volunteerType: UserVolunteerType?,
    val isMember: Boolean?,
    // true = booking with at least one shift, false = only meals or a bed
    val withVolunteering: Boolean?,
    val price: Double,
    val validFrom: LocalDate,
    // Exclusive; null = still in force
    val validTo: LocalDate?,
) {
    // `<n>` of `dash-<n>`, the last tiebreak
    val numericId: Int = id.substringAfterLast('-').toIntOrNull() ?: -1

    // How many criteria the rule sets: the more, the higher its priority
    val specificity: Int = listOf(volunteerType, isMember, withVolunteering).count { it != null }

    fun isInForce(date: LocalDate): Boolean = validFrom <= date && (validTo == null || date < validTo)

    fun matches(volunteerType: UserVolunteerType?, isMember: Boolean, withVolunteering: Boolean): Boolean =
        (this.volunteerType == null || this.volunteerType == volunteerType) &&
            (this.isMember == null || this.isMember == isMember) &&
            (this.withVolunteering == null || this.withVolunteering == withVolunteering)
}

/** One row of the dashboard's monthly quota table (`allowance_rules/dash-<n>`). */
data class AllowanceRule(
    val id: String,
    val volunteerType: UserVolunteerType,
    val freeMeals: Int,
    val freeSleeps: Int,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
) {
    val numericId: Int = id.substringAfterLast('-').toIntOrNull() ?: -1

    fun isInForce(date: LocalDate): Boolean = validFrom <= date && (validTo == null || date < validTo)
}

/**
 * The dashboard's full price tables, resolved per volunteer exactly as its `v_charge` view
 * does: among the rules for the item that match the volunteer and the booking and are in
 * force that day, the one with the most criteria wins, then the latest `valid_from`, then
 * the highest `dash-<n>`. No matching rule means 0 € (the dashboard flags it «sense preu»),
 * never the old hardcoded defaults.
 */
data class PricingRules(
    val prices: List<ItemPriceRule>,
    val allowances: List<AllowanceRule>,
) {
    fun priceOf(
        item: ChargeableItem,
        date: LocalDate,
        volunteerType: UserVolunteerType?,
        isMember: Boolean,
        withVolunteering: Boolean,
    ): Double =
        prices
            .filter { it.item == item && it.isInForce(date) && it.matches(volunteerType, isMember, withVolunteering) }
            .maxWithOrNull(
                compareBy<ItemPriceRule> { it.specificity }
                    .thenBy { it.validFrom }
                    .thenBy { it.numericId },
            )?.price ?: 0.0

    // Read at the 1st of the month by calculateMonthlyCharge, like v_charge_discounted
    fun allowanceAt(date: LocalDate, volunteerType: UserVolunteerType?): MitraAllowance =
        allowances
            .filter { it.volunteerType == volunteerType && it.isInForce(date) }
            .maxWithOrNull(compareBy<AllowanceRule> { it.validFrom }.thenBy { it.numericId })
            ?.let { MitraAllowance(freeMeals = it.freeMeals, freeSleeps = it.freeSleeps) }
            ?: MitraAllowance.None

    /**
     * The price timeline of one volunteer, in the shape calculateMonthlyCharge already
     * consumes: what any rule decides only changes on a `valid_from`/`valid_to`, so one
     * [PriceRule] per such boundary describes every date. Its allowance is the volunteer's
     * own type's (none for most types).
     */
    fun priceRulesFor(volunteerType: UserVolunteerType?, isMember: Boolean): PriceRules {
        val ownAllowances = allowances.filter { it.volunteerType == volunteerType }
        val boundaries = (
            listOf(EPOCH) +
                prices.flatMap { listOfNotNull(it.validFrom, it.validTo) } +
                ownAllowances.flatMap { listOfNotNull(it.validFrom, it.validTo) }
            ).distinct().sorted()

        return PriceRules(
            boundaries.map { date ->
                fun price(item: ChargeableItem, withVolunteering: Boolean) =
                    priceOf(item, date, volunteerType, isMember, withVolunteering)
                PriceRule(
                    validFrom = date,
                    prices = Prices(
                        lunch = price(ChargeableItem.Lunch, true),
                        dinner = price(ChargeableItem.Dinner, true),
                        breakfast = price(ChargeableItem.Breakfast, true),
                        breakfastNextDay = price(ChargeableItem.BreakfastNextDay, true),
                        sleep = price(ChargeableItem.Sleep, true),
                        mitraAllowance = allowanceAt(date, volunteerType),
                        withoutVolunteering = ItemPrices(
                            lunch = price(ChargeableItem.Lunch, false),
                            dinner = price(ChargeableItem.Dinner, false),
                            breakfast = price(ChargeableItem.Breakfast, false),
                            breakfastNextDay = price(ChargeableItem.BreakfastNextDay, false),
                            sleep = price(ChargeableItem.Sleep, false),
                        ),
                    ),
                )
            },
        )
    }

    private companion object {
        // Before the first rule: nothing is priced, like the dashboard
        val EPOCH = LocalDate(1970, 1, 1)
    }
}

/**
 * The price timeline [this] user is charged with: their own one from the full rule tables
 * or, when those couldn't be read ([pricing] null), the generic `price_rules` one, whose
 * allowance is the mitras' only.
 */
fun User.priceRules(pricing: PricingRules?, legacy: PriceRules): PriceRules =
    pricing?.priceRulesFor(volunteerType, isMember)
        ?: if (isMitra) legacy else legacy.withoutAllowance()
