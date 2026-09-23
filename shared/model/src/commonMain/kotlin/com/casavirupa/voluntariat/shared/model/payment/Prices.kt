package com.casavirupa.voluntariat.shared.model.payment

data class Prices(
    val lunch: Double = DEFAULT_MEAL_PRICE,
    val dinner: Double = DEFAULT_MEAL_PRICE,
    val breakfast: Double = DEFAULT_BREAKFAST_PRICE,
    val breakfastNextDay: Double = DEFAULT_BREAKFAST_PRICE,
    val sleep: Double = DEFAULT_SLEEP_PRICE,
    val mitraAllowance: MitraAllowance = MitraAllowance.None,
) {
    companion object {
        val Default = Prices()

        const val DEFAULT_MEAL_PRICE = 8.0
        const val DEFAULT_BREAKFAST_PRICE = 0.0
        const val DEFAULT_SLEEP_PRICE = 10.0
    }
}

// Monthly quota of items already covered by the mitra fee; unused units expire
// with the month, so they are applied as a derived discount and never stored.
// [freeMeals] is one pool shared by lunches, dinners and same-day breakfasts; a free
// night also covers that stay's next-day breakfast.
data class MitraAllowance(
    val freeMeals: Int = 0,
    val freeSleeps: Int = 0,
) {
    companion object {
        val None = MitraAllowance()
    }
}
