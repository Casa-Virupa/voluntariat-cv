package com.casavirupa.voluntariat.shared.model.payment

import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PricingRulesTest {

    private var nextId = 1

    private fun price(
        item: ChargeableItem,
        euros: Double,
        from: LocalDate,
        to: LocalDate? = null,
        type: UserVolunteerType? = null,
        member: Boolean? = null,
        withVolunteering: Boolean? = null,
        id: Int = nextId++,
    ) = ItemPriceRule(
        id = "dash-$id",
        item = item,
        volunteerType = type,
        isMember = member,
        withVolunteering = withVolunteering,
        price = euros,
        validFrom = from,
        validTo = to,
    )

    private val epoch = LocalDate(1970, 1, 1)
    private val jul1 = LocalDate(2026, 7, 1)
    private val aug2 = LocalDate(2026, 8, 2)
    private val sep1 = LocalDate(2026, 9, 1)
    private val sep21 = LocalDate(2026, 9, 21)

    private val mitra = UserVolunteerType.Mitra
    private val habitual = UserVolunteerType.Habitual

    // The production price table on 2026-10-02 (dashboard /configuracio › Preus)
    private val production = PricingRules(
        prices = listOf(
            price(ChargeableItem.Breakfast, 5.0, sep21, type = habitual),
            price(ChargeableItem.Breakfast, 3.0, sep1),
            price(ChargeableItem.Breakfast, 0.0, epoch, to = sep1),
            price(ChargeableItem.BreakfastNextDay, 0.0, epoch),
            price(ChargeableItem.Dinner, 8.0, aug2, type = habitual),
            price(ChargeableItem.Dinner, 5.0, aug2, type = mitra),
            price(ChargeableItem.Dinner, 4.0, jul1, to = aug2, type = mitra, member = true),
            price(ChargeableItem.Dinner, 8.0, epoch, to = aug2),
            price(ChargeableItem.Lunch, 8.0, aug2, type = habitual),
            price(ChargeableItem.Lunch, 5.0, aug2, type = mitra),
            price(ChargeableItem.Lunch, 4.0, jul1, to = aug2, type = mitra, member = true),
            price(ChargeableItem.Lunch, 8.0, epoch, to = aug2),
            price(ChargeableItem.Sleep, 10.0, aug2, type = habitual, member = true),
            price(ChargeableItem.Sleep, 5.0, aug2, type = mitra),
            price(ChargeableItem.Sleep, 10.0, epoch, to = aug2),
        ),
        allowances = listOf(
            AllowanceRule("dash-1", mitra, freeMeals = 4, freeSleeps = 2, validFrom = sep1, validTo = null),
        ),
    )

    private fun booking(
        date: LocalDate,
        vararg meals: Meal,
        sleep: Boolean = false,
        withVolunteering: Boolean = true,
    ) = Volunteer(
        id = VolunteerId(date.toString()),
        userId = UserId.Empty,
        date = date,
        shifts = if (withVolunteering) {
            listOf(Shift.Morning(type = VolunteerType.General, timeRange = TimeRange.DefaultMorning))
        } else {
            emptyList()
        },
        meals = meals.toList(),
        sleep = sleep,
    )

    private fun user(type: UserVolunteerType?, member: Boolean) = User(
        id = UserId("u"),
        name = "",
        email = "",
        role = UserRole.Unknown,
        volunteerType = type,
        hasOnboardingCompleted = true,
        specificAreas = emptyList(),
        isMember = member,
    )

    // Same as the app: one calculation per calendar month, summed
    private fun charges(bookings: List<Volunteer>, rules: PriceRules): Double =
        bookings.groupBy { it.date.year to it.date.month }.values
            .sumOf { calculateMonthlyCharge(it, rules).total }

    // Oscar's real bookings before October: the dashboard's opening balance was 68 €
    @Test
    fun aMitraMemberPaysTheMitraPricesAndTheQuotaFromSeptember() {
        val bookings = listOf(
            booking(LocalDate(2026, 7, 14), Meal.Lunch),
            booking(LocalDate(2026, 7, 30), Meal.Dinner),
            booking(LocalDate(2026, 8, 7), Meal.Lunch),
            booking(LocalDate(2026, 8, 12), Meal.Lunch),
            booking(LocalDate(2026, 8, 13), Meal.Lunch),
            booking(LocalDate(2026, 8, 16), Meal.Dinner),
            booking(LocalDate(2026, 8, 18), Meal.Lunch, Meal.Dinner, sleep = true),
            booking(LocalDate(2026, 8, 23), Meal.Lunch),
            booking(LocalDate(2026, 8, 27), Meal.Lunch, Meal.Dinner),
            booking(LocalDate(2026, 8, 28), Meal.Dinner, sleep = true),
            booking(LocalDate(2026, 9, 10), Meal.Breakfast, Meal.Lunch),
            booking(LocalDate(2026, 9, 19), Meal.Dinner),
            booking(LocalDate(2026, 9, 24), Meal.Dinner),
        )
        val rules = production.priceRulesFor(mitra, isMember = true)

        // July 2 × 4 €; August 10 meals + 2 nights × 5 €, no quota yet; September all free
        assertEquals(8.0, charges(bookings.filter { it.date.month.ordinal == 6 }, rules), TOLERANCE)
        assertEquals(60.0, charges(bookings.filter { it.date.month.ordinal == 7 }, rules), TOLERANCE)
        assertEquals(0.0, charges(bookings.filter { it.date.month.ordinal == 8 }, rules), TOLERANCE)
        assertEquals(68.0, charges(bookings, rules), TOLERANCE)
    }

    // Franc's real bookings before October: the dashboard's opening balance was 50 €
    @Test
    fun aMitraNightCostsTheMitraPriceToo() {
        val bookings = listOf(
            booking(LocalDate(2026, 8, 7), Meal.Dinner, sleep = true),
            booking(LocalDate(2026, 8, 8), Meal.Lunch, Meal.Dinner, sleep = true),
            booking(LocalDate(2026, 8, 9), Meal.Lunch),
            booking(LocalDate(2026, 8, 19), Meal.Lunch),
            booking(LocalDate(2026, 8, 28), Meal.Lunch, Meal.Dinner, sleep = true),
            booking(LocalDate(2026, 9, 13), Meal.Breakfast, Meal.Lunch, Meal.Dinner, sleep = true),
        )

        assertEquals(50.0, charges(bookings, production.priceRulesFor(mitra, isMember = true)), TOLERANCE)
    }

    // Jordi: 16 € in July on the generic prices, 8 € in October on the habitual one
    @Test
    fun aHabitualPaysTheGenericPricesUntilTheirOwnStart() {
        val rules = production.priceRulesFor(habitual, isMember = true)
        val bookings = listOf(
            booking(LocalDate(2026, 7, 17), Meal.Lunch, Meal.Dinner),
            booking(LocalDate(2026, 10, 17), Meal.Lunch),
        )

        assertEquals(24.0, charges(bookings, rules), TOLERANCE)
    }

    @Test
    fun theHabitualBreakfastPriceStartsOnItsOwnDate() {
        val rules = production.priceRulesFor(habitual, isMember = false)

        assertEquals(3.0, rules.priceAt(LocalDate(2026, 9, 20)).breakfast, TOLERANCE)
        assertEquals(5.0, rules.priceAt(sep21).breakfast, TOLERANCE)
        // a mitra keeps the generic 3 €
        assertEquals(3.0, production.priceRulesFor(mitra, isMember = true).priceAt(sep21).breakfast, TOLERANCE)
    }

    @Test
    fun withNoMatchingRuleAnItemCostsNothingLikeInTheDashboard() {
        // no type: lunch only has typed rules since 2026-08-02
        assertEquals(0.0, production.priceRulesFor(null, isMember = false).priceAt(aug2).lunch, TOLERANCE)
        // the habitual night is only priced for members
        assertEquals(0.0, production.priceRulesFor(habitual, isMember = false).priceAt(aug2).sleep, TOLERANCE)
        // before the first rule
        assertEquals(0.0, PricingRules(emptyList(), emptyList()).priceRulesFor(mitra, true).priceAt(aug2).lunch)
    }

    @Test
    fun theMostSpecificRuleWinsOverANewerOne() {
        val rules = PricingRules(
            prices = listOf(
                price(ChargeableItem.Lunch, 5.0, aug2, type = mitra),
                price(ChargeableItem.Lunch, 9.0, sep1),
                price(ChargeableItem.Lunch, 4.0, jul1, type = mitra, member = true),
            ),
            allowances = emptyList(),
        )

        assertEquals(4.0, rules.priceOf(ChargeableItem.Lunch, sep1, mitra, true, true), TOLERANCE)
        assertEquals(5.0, rules.priceOf(ChargeableItem.Lunch, sep1, mitra, false, true), TOLERANCE)
        assertEquals(9.0, rules.priceOf(ChargeableItem.Lunch, sep1, habitual, false, true), TOLERANCE)
    }

    @Test
    fun equalRulesAreSettledByTheLatestStartThenTheHighestId() {
        val rules = PricingRules(
            prices = listOf(
                price(ChargeableItem.Dinner, 6.0, jul1, id = 3),
                price(ChargeableItem.Dinner, 7.0, aug2, id = 1),
                price(ChargeableItem.Dinner, 8.0, aug2, id = 2),
            ),
            allowances = emptyList(),
        )

        assertEquals(8.0, rules.priceOf(ChargeableItem.Dinner, sep1, null, false, true), TOLERANCE)
        assertEquals(6.0, rules.priceOf(ChargeableItem.Dinner, jul1, null, false, true), TOLERANCE)
    }

    @Test
    fun aBookingWithoutVolunteeringUsesItsOwnRuleAndStaysOutOfTheQuota() {
        val rules = production.copy(
            prices = production.prices + price(ChargeableItem.Sleep, 15.0, sep1, withVolunteering = false),
        ).priceRulesFor(mitra, isMember = true)
        val bookings = listOf(booking(LocalDate(2026, 10, 1), sleep = true, withVolunteering = false))

        // the mitra 5 € rule is as specific but older; the quota never covers it
        assertEquals(15.0, charges(bookings, rules), TOLERANCE)
    }

    @Test
    fun theQuotaOnlyAppliesToItsOwnVolunteerType() {
        val october = LocalDate(2026, 10, 1)

        assertEquals(MitraAllowance(4, 2), production.priceRulesFor(mitra, true).priceAt(october).mitraAllowance)
        assertEquals(MitraAllowance.None, production.priceRulesFor(habitual, true).priceAt(october).mitraAllowance)
    }

    @Test
    fun theGenericTimelineIsTheFallbackAndOnlyMitrasKeepItsQuota() {
        val legacy = PriceRules(
            listOf(PriceRule(LocalDate(2020, 1, 1), Prices(mitraAllowance = MitraAllowance(4, 2)))),
        )

        assertEquals(legacy, user(mitra, true).priceRules(pricing = null, legacy = legacy))
        assertEquals(
            MitraAllowance.None,
            user(habitual, true).priceRules(pricing = null, legacy = legacy).priceAt(sep1).mitraAllowance,
        )
        assertEquals(
            5.0,
            user(mitra, true).priceRules(pricing = production, legacy = legacy).priceAt(sep1).lunch,
            TOLERANCE,
        )
    }

    companion object {
        private const val TOLERANCE = 0.0001
    }
}
