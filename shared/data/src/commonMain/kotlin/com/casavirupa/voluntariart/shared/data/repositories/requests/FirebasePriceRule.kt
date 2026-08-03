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
    val sleep: Double = Prices.DEFAULT_SLEEP_PRICE,
    @SerialName("mitra_free_lunches") val mitraFreeLunches: Int = 0,
    @SerialName("mitra_free_dinners") val mitraFreeDinners: Int = 0,
    @SerialName("mitra_free_sleeps") val mitraFreeSleeps: Int = 0,
)
