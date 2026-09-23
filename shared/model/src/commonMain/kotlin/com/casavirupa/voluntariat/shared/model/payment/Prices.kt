package com.casavirupa.voluntariat.shared.model.payment

data class Prices(
    val lunch: Double = DEFAULT_MEAL_PRICE,
    val dinner: Double = DEFAULT_MEAL_PRICE,
    val breakfast: Double = DEFAULT_BREAKFAST_PRICE,
    val breakfastNextDay: Double = DEFAULT_BREAKFAST_PRICE,
    val sleep: Double = DEFAULT_SLEEP_PRICE,
    val mitraAllowance: MitraAllowance = MitraAllowance.None,
    // Prices of a booking with no shifts (only meals or a bed, issue #93);
    // null = same as with volunteering.
    val withoutVolunteering: ItemPrices? = null,
) {
    /** The prices that apply to a booking, depending on whether it has any shift. */
    fun forBooking(withVolunteering: Boolean): Prices =
        if (withVolunteering || withoutVolunteering == null) {
            this
        } else {
            copy(
                lunch = withoutVolunteering.lunch ?: lunch,
                dinner = withoutVolunteering.dinner ?: dinner,
                breakfast = withoutVolunteering.breakfast ?: breakfast,
                breakfastNextDay = withoutVolunteering.breakfastNextDay ?: breakfastNextDay,
                sleep = withoutVolunteering.sleep ?: sleep,
            )
        }

    companion object {
        val Default = Prices()

        const val DEFAULT_MEAL_PRICE = 8.0
        const val DEFAULT_BREAKFAST_PRICE = 0.0
        const val DEFAULT_SLEEP_PRICE = 10.0
    }
}

// Per-item overrides; a null item keeps the ordinary price.
data class ItemPrices(
    val lunch: Double? = null,
    val dinner: Double? = null,
    val breakfast: Double? = null,
    val breakfastNextDay: Double? = null,
    val sleep: Double? = null,
)

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
