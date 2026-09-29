package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.User

/** Meals and night a booking may include. */
data class BookableServices(
    val meals: Set<Meal>,
    val sleep: Boolean,
) {
    /**
     * Drops the breakfast a neighbouring booking already covers, so a morning never gets two
     * (issue #86): the previous day's «esmorzar de l'endemà» is this day's breakfast, and this
     * day's next-day breakfast is the following day's.
     */
    fun withoutDuplicateBreakfasts(previousDay: Volunteer?, nextDay: Volunteer?): BookableServices =
        copy(
            meals = meals.filterNot { meal ->
                when (meal) {
                    Meal.Breakfast -> previousDay?.meals.orEmpty().contains(Meal.BreakfastNextDay)
                    Meal.BreakfastNextDay -> nextDay?.meals.orEmpty().contains(Meal.Breakfast)
                    else -> false
                }
            }.toSet(),
        )

    companion object {
        val All = BookableServices(
            meals = setOf(Meal.Breakfast, Meal.Lunch, Meal.Dinner, Meal.BreakfastNextDay),
            sleep = true,
        )

        /**
         * What a non-mitra's on-site volunteering justifies. A morning shift covers that day's
         * breakfast and lunch (the night before is booked on the previous day, see [nextDay]);
         * an afternoon shift covers lunch, dinner, the night and the next morning's breakfast.
         * Online shifts don't count: the volunteer isn't at the house.
         *
         * @param nextDay the user's booking on the following day, if any: an on-site morning
         * shift there lets them sleep the night before.
         */
        fun forVolunteering(
            morning: Boolean,
            afternoon: Boolean,
            nextDay: Volunteer?,
        ): BookableServices {
            val nextDayMorning = nextDay?.shifts.orEmpty().any { it is Shift.Morning && !it.online }
            val sleep = afternoon || nextDayMorning
            val meals = buildSet {
                if (morning) add(Meal.Breakfast)
                if (morning || afternoon) add(Meal.Lunch)
                if (afternoon) add(Meal.Dinner)
                if (sleep) add(Meal.BreakfastNextDay)
            }
            return BookableServices(meals = meals, sleep = sleep)
        }
    }
}

/**
 * Mitras may book meals and nights freely, even without volunteering that day (issue #108);
 * everyone else only what their on-site shifts justify. Either way, a breakfast already booked
 * by the previous or next day's booking isn't offered again.
 *
 * @param previousDay / [nextDay] the user's own bookings on the neighbouring days, if any.
 */
fun User.bookableServices(
    morning: Boolean,
    afternoon: Boolean,
    nextDay: Volunteer?,
    previousDay: Volunteer? = null,
): BookableServices =
    (if (isMitra) BookableServices.All else BookableServices.forVolunteering(morning, afternoon, nextDay))
        .withoutDuplicateBreakfasts(previousDay = previousDay, nextDay = nextDay)
