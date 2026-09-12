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
    val freeLunchesUsed: Int,
    val freeDinnersUsed: Int,
    val freeNightsUsed: Int,
    val lunchesDiscount: Double,
    val dinnersDiscount: Double,
    val nightsDiscount: Double,
) {
    val discount: Double get() = lunchesDiscount + dinnersDiscount + nightsDiscount

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
            freeLunchesUsed = 0,
            freeDinnersUsed = 0,
            freeNightsUsed = 0,
            lunchesDiscount = 0.0,
            dinnersDiscount = 0.0,
            nightsDiscount = 0.0,
        )
    }
}

/**
 * Net charge for one calendar month of bookings. For mitra volunteers the first
 * N chronological lunches/dinners/nights are free, each zeroed at its own
 * booking-date price. The allowance is the one in force on the 1st of the month,
 * so a rule change mid-month only affects the following month. [volunteers] must
 * all belong to the same calendar month.
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
    var freeLunchesUsed = 0
    var freeDinnersUsed = 0
    var freeNightsUsed = 0
    var lunchesDiscount = 0.0
    var dinnersDiscount = 0.0
    var nightsDiscount = 0.0

    volunteers.sortedBy { it.date }.forEach { volunteer ->
        val prices = priceRules.priceAt(volunteer.date)
        volunteer.meals.forEach { meal ->
            when (meal) {
                Meal.Lunch -> {
                    lunches++
                    lunchesAmount += prices.lunch
                    if (freeLunchesUsed < allowance.freeLunches) {
                        freeLunchesUsed++
                        lunchesDiscount += prices.lunch
                    }
                }
                Meal.Dinner -> {
                    dinners++
                    dinnersAmount += prices.dinner
                    if (freeDinnersUsed < allowance.freeDinners) {
                        freeDinnersUsed++
                        dinnersDiscount += prices.dinner
                    }
                }
                Meal.Breakfast -> {
                    breakfasts++
                    breakfastsAmount += prices.breakfast
                }
                Meal.BreakfastNextDay -> {
                    breakfastsNextDay++
                    breakfastsNextDayAmount += prices.breakfastNextDay
                }
                Meal.Unknown -> Unit
            }
        }
        if (volunteer.sleep) {
            nights++
            nightsAmount += prices.sleep
            if (freeNightsUsed < allowance.freeSleeps) {
                freeNightsUsed++
                nightsDiscount += prices.sleep
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
        freeLunchesUsed = freeLunchesUsed,
        freeDinnersUsed = freeDinnersUsed,
        freeNightsUsed = freeNightsUsed,
        lunchesDiscount = lunchesDiscount,
        dinnersDiscount = dinnersDiscount,
        nightsDiscount = nightsDiscount,
    )
}
