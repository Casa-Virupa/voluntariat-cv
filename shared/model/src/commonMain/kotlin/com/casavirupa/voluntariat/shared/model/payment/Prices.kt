package com.casavirupa.voluntariat.shared.model.payment

data class Prices(
    val lunch: Double = DEFAULT_MEAL_PRICE,
    val dinner: Double = DEFAULT_MEAL_PRICE,
    val breakfast: Double = DEFAULT_BREAKFAST_PRICE,
    val sleep: Double = DEFAULT_SLEEP_PRICE,
) {
    companion object {
        val Default = Prices()

        const val DEFAULT_MEAL_PRICE = 8.0
        const val DEFAULT_BREAKFAST_PRICE = 0.0
        const val DEFAULT_SLEEP_PRICE = 10.0
    }
}
