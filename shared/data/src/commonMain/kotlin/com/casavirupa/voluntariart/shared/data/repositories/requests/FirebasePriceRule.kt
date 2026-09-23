package com.casavirupa.voluntariart.shared.data.repositories.requests

import com.casavirupa.voluntariat.shared.model.payment.Prices
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebasePriceRule(
    @SerialName("valid_from") val validFrom: String = "",
    val lunch: Double = Prices.DEFAULT_MEAL_PRICE,
    val dinner: Double = Prices.DEFAULT_MEAL_PRICE,
    val breakfast: Double = Prices.DEFAULT_BREAKFAST_PRICE,
    @SerialName("breakfast_next_day") val breakfastNextDay: Double = Prices.DEFAULT_BREAKFAST_PRICE,
    val sleep: Double = Prices.DEFAULT_SLEEP_PRICE,
    // Prices of a booking with no shifts (issue #93); absent = same as with volunteering
    @SerialName("lunch_no_volunteering") val lunchNoVolunteering: Double? = null,
    @SerialName("dinner_no_volunteering") val dinnerNoVolunteering: Double? = null,
    @SerialName("breakfast_no_volunteering") val breakfastNoVolunteering: Double? = null,
    @SerialName("breakfast_next_day_no_volunteering") val breakfastNextDayNoVolunteering: Double? = null,
    @SerialName("sleep_no_volunteering") val sleepNoVolunteering: Double? = null,
    // Shared mitra meal pool (lunch, dinner, same-day breakfast). Docs published before it
    // existed only carry the per-item lunch/dinner quotas, which then add up to the pool.
    @SerialName("mitra_free_meals") val mitraFreeMeals: Int? = null,
    @SerialName("mitra_free_lunches") val mitraFreeLunches: Int = 0,
    @SerialName("mitra_free_dinners") val mitraFreeDinners: Int = 0,
    @SerialName("mitra_free_sleeps") val mitraFreeSleeps: Int = 0,
)
