package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.User

/** Meals and night a booking may include. */
data class BookableServices(
    val meals: Set<Meal>,
    val sleep: Boolean,
) {
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
         * shift there lets them sleep the night before (and have breakfast, unless that booking
         * already has it).
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
                if (sleep && nextDay?.meals.orEmpty().none { it == Meal.Breakfast }) {
                    add(Meal.BreakfastNextDay)
                }
            }
            return BookableServices(meals = meals, sleep = sleep)
        }
    }
}

/**
 * Mitras may book meals and nights freely, even without volunteering that day (issue #108);
 * everyone else only what their on-site shifts justify.
 */
fun User.bookableServices(
    morning: Boolean,
    afternoon: Boolean,
    nextDay: Volunteer?,
): BookableServices =
    if (isMitra) BookableServices.All
    else BookableServices.forVolunteering(morning, afternoon, nextDay)
