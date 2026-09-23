package com.casavirupa.voluntariat.shared.model.payment

import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import kotlinx.datetime.LocalDate

data class MonthlyChargeBreakdown(
    val lunches: Int,
    val dinners: Int,
    val nights: Int,
    val breakfasts: Int,
    val breakfastsNextDay: Int,
    val lunchesAmount: Double,
    val dinnersAmount: Double,
    val nightsAmount: Double,
    val breakfastsAmount: Double,
    val breakfastsNextDayAmount: Double,
    // Lunches, dinners and same-day breakfasts covered by the mitra meal pool
    val freeMealsUsed: Int,
    val freeNightsUsed: Int,
    // Next-day breakfasts that come with a free night
    val freeBreakfastsNextDayUsed: Int,
    val mealsDiscount: Double,
    val nightsDiscount: Double,
    val breakfastsNextDayDiscount: Double,
) {
    val discount: Double get() = mealsDiscount + nightsDiscount + breakfastsNextDayDiscount

    val total: Double
        get() = lunchesAmount + dinnersAmount + nightsAmount +
            breakfastsAmount + breakfastsNextDayAmount - discount

    companion object {
        val Empty = MonthlyChargeBreakdown(
            lunches = 0,
            dinners = 0,
            nights = 0,
            breakfasts = 0,
            breakfastsNextDay = 0,
            lunchesAmount = 0.0,
            dinnersAmount = 0.0,
            nightsAmount = 0.0,
            breakfastsAmount = 0.0,
            breakfastsNextDayAmount = 0.0,
            freeMealsUsed = 0,
            freeNightsUsed = 0,
            freeBreakfastsNextDayUsed = 0,
            mealsDiscount = 0.0,
            nightsDiscount = 0.0,
            breakfastsNextDayDiscount = 0.0,
        )
    }
}

/**
 * Net charge for one calendar month of bookings. For mitra volunteers:
 * - the first N chronological meals (lunch, dinner or same-day breakfast, one shared pool)
 *   are free, and so are the first M nights; each unit is zeroed at its own booking-date
 *   price, and an item that costs nothing never uses up a unit;
 * - a free night also makes that stay's next-day breakfast free;
 * - a booking without volunteering (no shifts: only meals or a bed) is priced with the
 *   "without volunteering" prices and stays outside the allowance altogether.
 * Order: booking date, then booking id, then breakfast → lunch → dinner within the day
 * (never the order the options were tapped in) — exactly what the dashboard's
 * `v_charge_discounted` does. The allowance is the one in force on the 1st of the month,
 * so a rule change mid-month only affects the following month. [volunteers] must all
 * belong to the same calendar month.
 */
fun calculateMonthlyCharge(
    volunteers: List<Volunteer>,
    priceRules: PriceRules,
    isMitra: Boolean,
): MonthlyChargeBreakdown {
    if (volunteers.isEmpty()) return MonthlyChargeBreakdown.Empty

    val firstDate = volunteers.first().date
    val allowance = if (isMitra) {
        priceRules.priceAt(LocalDate(firstDate.year, firstDate.month, 1)).mitraAllowance
    } else {
        MitraAllowance.None
    }

    var lunches = 0
    var dinners = 0
    var nights = 0
    var breakfasts = 0
    var breakfastsNextDay = 0
    var lunchesAmount = 0.0
    var dinnersAmount = 0.0
    var nightsAmount = 0.0
    var breakfastsAmount = 0.0
    var breakfastsNextDayAmount = 0.0
    var freeMealsUsed = 0
    var freeNightsUsed = 0
    var freeBreakfastsNextDayUsed = 0
    var mealsDiscount = 0.0
    var nightsDiscount = 0.0
    var breakfastsNextDayDiscount = 0.0

    fun takeFreeMeal(price: Double) {
        if (price > 0.0 && freeMealsUsed < allowance.freeMeals) {
            freeMealsUsed++
            mealsDiscount += price
        }
    }

    volunteers
        .sortedWith(compareBy<Volunteer> { it.date }.thenBy { it.id.value })
        .forEach { volunteer ->
            val withVolunteering = volunteer.shifts.isNotEmpty()
            val prices = priceRules.priceAt(volunteer.date).forBooking(withVolunteering)
            var nightIsFree = false
            if (volunteer.sleep) {
                nights++
                nightsAmount += prices.sleep
                if (withVolunteering && prices.sleep > 0.0 && freeNightsUsed < allowance.freeSleeps) {
                    freeNightsUsed++
                    nightsDiscount += prices.sleep
                    nightIsFree = true
                }
            }
            volunteer.meals.sortedBy { it.dayOrder() }.forEach { meal ->
                when (meal) {
                    Meal.Breakfast -> {
                        breakfasts++
                        breakfastsAmount += prices.breakfast
                        if (withVolunteering) takeFreeMeal(prices.breakfast)
                    }
                    Meal.Lunch -> {
                        lunches++
                        lunchesAmount += prices.lunch
                        if (withVolunteering) takeFreeMeal(prices.lunch)
                    }
                    Meal.Dinner -> {
                        dinners++
                        dinnersAmount += prices.dinner
                        if (withVolunteering) takeFreeMeal(prices.dinner)
                    }
                    Meal.BreakfastNextDay -> {
                        breakfastsNextDay++
                        breakfastsNextDayAmount += prices.breakfastNextDay
                        if (nightIsFree && prices.breakfastNextDay > 0.0) {
                            freeBreakfastsNextDayUsed++
                            breakfastsNextDayDiscount += prices.breakfastNextDay
                        }
                    }
                    Meal.Unknown -> Unit
                }
            }
        }

    return MonthlyChargeBreakdown(
        lunches = lunches,
        dinners = dinners,
        nights = nights,
        breakfasts = breakfasts,
        breakfastsNextDay = breakfastsNextDay,
        lunchesAmount = lunchesAmount,
        dinnersAmount = dinnersAmount,
        nightsAmount = nightsAmount,
        breakfastsAmount = breakfastsAmount,
        breakfastsNextDayAmount = breakfastsNextDayAmount,
        freeMealsUsed = freeMealsUsed,
        freeNightsUsed = freeNightsUsed,
        freeBreakfastsNextDayUsed = freeBreakfastsNextDayUsed,
        mealsDiscount = mealsDiscount,
        nightsDiscount = nightsDiscount,
        breakfastsNextDayDiscount = breakfastsNextDayDiscount,
    )
}

// Chronological order of a day's meals; the dashboard ranks them the same way.
private fun Meal.dayOrder(): Int =
    when (this) {
        Meal.Breakfast -> 0
        Meal.Lunch -> 1
        Meal.Dinner -> 2
        Meal.BreakfastNextDay -> 3
        Meal.Unknown -> 4
    }
