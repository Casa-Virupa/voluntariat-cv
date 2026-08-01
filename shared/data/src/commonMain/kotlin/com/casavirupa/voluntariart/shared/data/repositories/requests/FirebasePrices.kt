package com.casavirupa.voluntariart.shared.data.repositories.requests

import com.casavirupa.voluntariat.shared.model.payment.Prices
import kotlinx.serialization.Serializable

@Serializable
data class FirebasePrices(
    val lunch: Double = Prices.DEFAULT_MEAL_PRICE,
    val dinner: Double = Prices.DEFAULT_MEAL_PRICE,
    val breakfast: Double = Prices.DEFAULT_BREAKFAST_PRICE,
    val sleep: Double = Prices.DEFAULT_SLEEP_PRICE,
)
