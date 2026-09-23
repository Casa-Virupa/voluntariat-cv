package com.casavirupa.voluntariat.shared.model.payment

import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthlyChargeTest {

    private val allowance42 = MitraAllowance(freeMeals = 4, freeSleeps = 2)

    private val defaultRules = rules(
        PriceRule(
            validFrom = LocalDate(2020, 1, 1),
            prices = Prices(mitraAllowance = allowance42),
        ),
    )

    private fun rules(vararg rules: PriceRule) = PriceRules(rules.toList())

    private fun booking(
        date: LocalDate,
        meals: List<Meal> = emptyList(),
        sleep: Boolean = false,
        id: String = "",
    ) = Volunteer(
        id = VolunteerId(id),
        userId = UserId.Empty,
        date = date,
        shifts = emptyList(),
        meals = meals,
        sleep = sleep,
    )

    @Test
    fun nonMitraPaysGrossWithNoDiscount() {
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 3), meals = listOf(Meal.Lunch, Meal.Dinner), sleep = true),
            booking(LocalDate(2026, 8, 10), meals = listOf(Meal.Lunch)),
        )

        val result = calculateMonthlyCharge(volunteers, defaultRules, isMitra = false)

        assertEquals(0.0, result.discount, TOLERANCE)
        assertEquals(0, result.freeMealsUsed)
        assertEquals(0, result.freeNightsUsed)
        assertEquals(2 * 8.0 + 8.0 + 10.0, result.total, TOLERANCE)
    }

    @Test
    fun mitraUnderAllowancePaysNothing() {
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.Lunch), sleep = true),
        )

        val result = calculateMonthlyCharge(volunteers, defaultRules, isMitra = true)

        assertEquals(1, result.freeMealsUsed)
        assertEquals(1, result.freeNightsUsed)
        assertEquals(0.0, result.total, TOLERANCE)
    }

    // Issue #90: day volunteers who don't stay for dinner were charged for a 3rd lunch
    @Test
    fun threeLunchesAndNoDinnerAreAllInsideTheMealPool() {
        val volunteers = (1..3).map { day ->
            booking(LocalDate(2026, 10, day * 7), meals = listOf(Meal.Lunch))
        }

        val result = calculateMonthlyCharge(volunteers, defaultRules, isMitra = true)

        assertEquals(3, result.freeMealsUsed)
        assertEquals(3 * 8.0, result.mealsDiscount, TOLERANCE)
        assertEquals(0.0, result.total, TOLERANCE)
    }

    @Test
    fun mitraOverAllowanceGetsFourMealsAndTwoNightsFree() {
        val volunteers = (1..3).map { day ->
            booking(
                LocalDate(2026, 8, day),
                meals = listOf(Meal.Lunch, Meal.Dinner),
                sleep = true,
            )
        }

        val result = calculateMonthlyCharge(volunteers, defaultRules, isMitra = true)

        assertEquals(4, result.freeMealsUsed)
        assertEquals(2, result.freeNightsUsed)
        assertEquals(4 * 8.0 + 2 * 10.0, result.discount, TOLERANCE)
        // day 3's lunch + dinner and its night are charged
        assertEquals(8.0 + 8.0 + 10.0, result.total, TOLERANCE)
    }

    @Test
    fun mealsOfADayTakeThePoolInBreakfastLunchDinnerOrderNotTapOrder() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(
                    lunch = 10.0,
                    dinner = 8.0,
                    mitraAllowance = MitraAllowance(freeMeals = 1),
                ),
            ),
        )
        // Dinner tapped before lunch: the lunch still comes first in the day
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.Dinner, Meal.Lunch)),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        assertEquals(10.0, result.mealsDiscount, TOLERANCE)
        assertEquals(8.0, result.total, TOLERANCE)
    }

    @Test
    fun sameDayBookingsAreOrderedByBookingId() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(
                    lunch = 8.0,
                    dinner = 5.0,
                    mitraAllowance = MitraAllowance(freeMeals = 1),
                ),
            ),
        )
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.Lunch), id = "b"),
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.Dinner), id = "a"),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        // booking "a" (the dinner) is first, so it takes the only free meal
        assertEquals(5.0, result.mealsDiscount, TOLERANCE)
        assertEquals(8.0, result.total, TOLERANCE)
    }

    @Test
    fun discountUsesEachItemsOwnBookingDatePriceInChronologicalOrder() {
        val allowance = MitraAllowance(freeMeals = 2)
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(lunch = 8.0, mitraAllowance = allowance),
            ),
            PriceRule(
                validFrom = LocalDate(2026, 8, 15),
                prices = Prices(lunch = 10.0, mitraAllowance = allowance),
            ),
        )
        // Passed out of order on purpose: the function must sort by date.
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 25), meals = listOf(Meal.Lunch)),
            booking(LocalDate(2026, 8, 3), meals = listOf(Meal.Lunch)),
            booking(LocalDate(2026, 8, 20), meals = listOf(Meal.Lunch)),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        assertEquals(8.0 + 10.0, result.mealsDiscount, TOLERANCE)
        assertEquals(10.0, result.total, TOLERANCE)
    }

    @Test
    fun allowanceComesFromRuleInForceOnFirstOfMonth() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(mitraAllowance = allowance42),
            ),
            PriceRule(
                validFrom = LocalDate(2026, 8, 15),
                prices = Prices(mitraAllowance = MitraAllowance(freeMeals = 6)),
            ),
        )
        val august = (1..6).map { day ->
            booking(LocalDate(2026, 8, day * 4), meals = listOf(Meal.Lunch))
        }
        val september = (1..6).map { day ->
            booking(LocalDate(2026, 9, day * 4), meals = listOf(Meal.Lunch))
        }

        // Mid-month rule change does not apply to August (allowance read on the 1st)...
        assertEquals(4, calculateMonthlyCharge(august, rules, isMitra = true).freeMealsUsed)
        // ...but does apply from September on.
        assertEquals(6, calculateMonthlyCharge(september, rules, isMitra = true).freeMealsUsed)
    }

    @Test
    fun emptyMonthHasNoCharge() {
        val result = calculateMonthlyCharge(emptyList(), defaultRules, isMitra = true)

        assertEquals(MonthlyChargeBreakdown.Empty, result)
        assertEquals(0.0, result.total, TOLERANCE)
    }

    @Test
    fun pricedSameDayBreakfastTakesAMealFromThePool() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(breakfast = 3.0, mitraAllowance = MitraAllowance(freeMeals = 1)),
            ),
        )
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.Lunch, Meal.Breakfast)),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        // breakfast comes first in the day, so it is the free one
        assertEquals(1, result.breakfasts)
        assertEquals(1, result.freeMealsUsed)
        assertEquals(3.0, result.mealsDiscount, TOLERANCE)
        assertEquals(8.0, result.total, TOLERANCE)
    }

    @Test
    fun freeBreakfastNeverUsesUpTheMealPool() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(breakfast = 0.0, mitraAllowance = MitraAllowance(freeMeals = 1)),
            ),
        )
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.Breakfast, Meal.Lunch)),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        assertEquals(1, result.freeMealsUsed)
        assertEquals(8.0, result.mealsDiscount, TOLERANCE)
        assertEquals(0.0, result.total, TOLERANCE)
    }

    @Test
    fun nextDayBreakfastIsFreeOnlyWithAFreeNight() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(
                    breakfastNextDay = 4.5,
                    mitraAllowance = MitraAllowance(freeMeals = 4, freeSleeps = 1),
                ),
            ),
        )
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.BreakfastNextDay), sleep = true),
            booking(LocalDate(2026, 8, 6), meals = listOf(Meal.BreakfastNextDay), sleep = true),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        assertEquals(2, result.breakfastsNextDay)
        assertEquals(1, result.freeNightsUsed)
        assertEquals(1, result.freeBreakfastsNextDayUsed)
        assertEquals(0, result.freeMealsUsed)
        assertEquals(4.5, result.breakfastsNextDayDiscount, TOLERANCE)
        // the 6th's night and its breakfast are outside the quota
        assertEquals(10.0 + 4.5, result.total, TOLERANCE)
    }

    @Test
    fun allowanceDoesNotCarryOverBetweenMonths() {
        val january = emptyList<Volunteer>()
        val february = (1..5).map { day ->
            booking(LocalDate(2026, 2, day), meals = listOf(Meal.Lunch))
        }

        // January's unused allowance must not grow February's: still only 4 free.
        assertEquals(0.0, calculateMonthlyCharge(january, defaultRules, isMitra = true).total, TOLERANCE)
        val result = calculateMonthlyCharge(february, defaultRules, isMitra = true)
        assertEquals(4, result.freeMealsUsed)
        assertEquals(8.0, result.total, TOLERANCE)
    }

    companion object {
        private const val TOLERANCE = 0.0001
    }
}
