package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseAllowanceRule
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseItemPriceRule
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebasePriceRule
import com.casavirupa.voluntariat.shared.domain.PriceRepository
import com.casavirupa.voluntariat.shared.model.payment.AllowanceRule
import com.casavirupa.voluntariat.shared.model.payment.ChargeableItem
import com.casavirupa.voluntariat.shared.model.payment.ItemPriceRule
import com.casavirupa.voluntariat.shared.model.payment.ItemPrices
import com.casavirupa.voluntariat.shared.model.payment.MitraAllowance
import com.casavirupa.voluntariat.shared.model.payment.PriceRule
import com.casavirupa.voluntariat.shared.model.payment.PriceRules
import com.casavirupa.voluntariat.shared.model.payment.Prices
import com.casavirupa.voluntariat.shared.model.payment.PricingRules
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FirebasePriceRepository(
    private val firestore: FirebaseFirestore,
) : PriceRepository {
    override fun getPriceRules(): Flow<PriceRules> =
        firestore
            .collection("price_rules")
            .snapshots
            .map { snapshot ->
                PriceRules(
                    snapshot.documents.mapNotNull { doc ->
                        runCatching { doc.data<FirebasePriceRule>() }
                            .getOrNull()
                            ?.toDomainModelOrNull()
                    },
                )
            }.catch { emit(PriceRules.Empty) }

    // An empty item_price_rules means the dashboard hasn't published it yet: null, so the
    // app keeps the generic timeline instead of pricing everything at 0 €.
    override fun getPricingRules(): Flow<PricingRules?> =
        combine(
            firestore.collection("item_price_rules").snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.data<FirebaseItemPriceRule>() }
                        .getOrNull()
                        ?.toDomainModelOrNull(doc.id)
                }
            },
            firestore.collection("allowance_rules").snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.data<FirebaseAllowanceRule>() }
                        .getOrNull()
                        ?.toDomainModelOrNull(doc.id)
                }
            },
        ) { prices, allowances ->
            val rules: PricingRules? = prices.takeIf { it.isNotEmpty() }?.let { PricingRules(it, allowances) }
            rules
        }.catch { emit(null) }
}

private fun FirebaseItemPriceRule.toDomainModelOrNull(id: String): ItemPriceRule? {
    val chargeableItem = when (item) {
        "lunch" -> ChargeableItem.Lunch
        "dinner" -> ChargeableItem.Dinner
        "breakfast" -> ChargeableItem.Breakfast
        "breakfast_next_day" -> ChargeableItem.BreakfastNextDay
        "sleep" -> ChargeableItem.Sleep
        else -> null
    } ?: return null
    // A type the app doesn't know can't match any user here; dropping it beats reading it as "any"
    val type = volunteerType?.let { it.toVolunteerType() ?: return null }
    if (price < 0.0) return null
    val from = runCatching { LocalDate.parse(validFrom) }.getOrNull() ?: return null
    val to = validTo?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return null }
    return ItemPriceRule(
        id = id,
        item = chargeableItem,
        volunteerType = type,
        isMember = isMember,
        withVolunteering = withVolunteering,
        price = price,
        validFrom = from,
        validTo = to,
    )
}

private fun FirebaseAllowanceRule.toDomainModelOrNull(id: String): AllowanceRule? {
    val type = volunteerType.toVolunteerType() ?: return null
    if (freeMeals < 0 || freeSleeps < 0) return null
    val from = runCatching { LocalDate.parse(validFrom) }.getOrNull() ?: return null
    val to = validTo?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return null }
    return AllowanceRule(
        id = id,
        volunteerType = type,
        freeMeals = freeMeals,
        freeSleeps = freeSleeps,
        validFrom = from,
        validTo = to,
    )
}

private fun FirebasePriceRule.toDomainModelOrNull(): PriceRule? =
    runCatching { LocalDate.parse(validFrom) }
        .getOrNull()
        ?.let { date ->
            PriceRule(
                validFrom = date,
                prices = Prices(
                    lunch = lunch,
                    dinner = dinner,
                    breakfast = breakfast,
                    breakfastNextDay = breakfastNextDay,
                    sleep = sleep,
                    mitraAllowance = MitraAllowance(
                        freeMeals = mitraFreeMeals ?: (mitraFreeLunches + mitraFreeDinners),
                        freeSleeps = mitraFreeSleeps,
                    ),
                    withoutVolunteering = ItemPrices(
                        lunch = lunchNoVolunteering,
                        dinner = dinnerNoVolunteering,
                        breakfast = breakfastNoVolunteering,
                        breakfastNextDay = breakfastNextDayNoVolunteering,
                        sleep = sleepNoVolunteering,
                    ),
                ),
            )
        }
