package com.casavirupa.voluntariat.shared.model.payment

import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthlyChargeTest {

    private val allowance222 = MitraAllowance(freeLunches = 2, freeDinners = 2, freeSleeps = 2)

    private val defaultRules = rules(
        PriceRule(
            validFrom = LocalDate(2020, 1, 1),
            prices = Prices(mitraAllowance = allowance222),
        ),
    )

    private fun rules(vararg rules: PriceRule) = PriceRules(rules.toList())

    private fun booking(
        date: LocalDate,
        meals: List<Meal> = emptyList(),
        sleep: Boolean = false,
    ) = Volunteer(
        id = VolunteerId.Empty,
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
        assertEquals(0, result.freeLunchesUsed)
        assertEquals(0, result.freeDinnersUsed)
        assertEquals(0, result.freeNightsUsed)
        assertEquals(2 * 8.0 + 8.0 + 10.0, result.total, TOLERANCE)
    }

    @Test
    fun mitraUnderAllowancePaysNothing() {
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.Lunch), sleep = true),
        )

        val result = calculateMonthlyCharge(volunteers, defaultRules, isMitra = true)

        assertEquals(1, result.freeLunchesUsed)
        assertEquals(1, result.freeNightsUsed)
        assertEquals(0.0, result.total, TOLERANCE)
    }

    @Test
    fun mitraOverAllowanceGetsExactlyTwoOfEachFree() {
        val volunteers = (1..3).map { day ->
            booking(
                LocalDate(2026, 8, day),
                meals = listOf(Meal.Lunch, Meal.Dinner),
                sleep = true,
            )
        }

        val result = calculateMonthlyCharge(volunteers, defaultRules, isMitra = true)

        assertEquals(2, result.freeLunchesUsed)
        assertEquals(2, result.freeDinnersUsed)
        assertEquals(2, result.freeNightsUsed)
        assertEquals(2 * 8.0 + 2 * 8.0 + 2 * 10.0, result.discount, TOLERANCE)
        assertEquals(8.0 + 8.0 + 10.0, result.total, TOLERANCE)
    }

    @Test
    fun discountUsesEachItemsOwnBookingDatePriceInChronologicalOrder() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(lunch = 8.0, mitraAllowance = allowance222),
            ),
            PriceRule(
                validFrom = LocalDate(2026, 8, 15),
                prices = Prices(lunch = 10.0, mitraAllowance = allowance222),
            ),
        )
        // Passed out of order on purpose: the function must sort by date.
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 25), meals = listOf(Meal.Lunch)),
            booking(LocalDate(2026, 8, 3), meals = listOf(Meal.Lunch)),
            booking(LocalDate(2026, 8, 20), meals = listOf(Meal.Lunch)),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        assertEquals(8.0 + 10.0, result.lunchesDiscount, TOLERANCE)
        assertEquals(10.0, result.total, TOLERANCE)
    }

    @Test
    fun allowanceComesFromRuleInForceOnFirstOfMonth() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(mitraAllowance = allowance222),
            ),
            PriceRule(
                validFrom = LocalDate(2026, 8, 15),
                prices = Prices(mitraAllowance = MitraAllowance(freeLunches = 4)),
            ),
        )
        val august = (1..4).map { day ->
            booking(LocalDate(2026, 8, day * 5), meals = listOf(Meal.Lunch))
        }
        val september = (1..4).map { day ->
            booking(LocalDate(2026, 9, day * 5), meals = listOf(Meal.Lunch))
        }

        // Mid-month rule change does not apply to August (allowance read on the 1st)...
        assertEquals(2, calculateMonthlyCharge(august, rules, isMitra = true).freeLunchesUsed)
        // ...but does apply from September on.
        assertEquals(4, calculateMonthlyCharge(september, rules, isMitra = true).freeLunchesUsed)
    }

    @Test
    fun emptyMonthHasNoCharge() {
        val result = calculateMonthlyCharge(emptyList(), defaultRules, isMitra = true)

        assertEquals(MonthlyChargeBreakdown.Empty, result)
        assertEquals(0.0, result.total, TOLERANCE)
    }

    @Test
    fun breakfastsAreChargedButNeverDiscounted() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(breakfast = 3.0, mitraAllowance = allowance222),
            ),
        )
        val volunteers = listOf(
            booking(LocalDate(2026, 8, 5), meals = listOf(Meal.Breakfast, Meal.Lunch)),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        assertEquals(1, result.breakfasts)
        assertEquals(3.0, result.breakfastsAmount, TOLERANCE)
        assertEquals(0.0, result.discount - result.lunchesDiscount, TOLERANCE)
        assertEquals(3.0, result.total, TOLERANCE)
    }

    @Test
    fun nextDayBreakfastUsesItsOwnPriceAndIsNeverDiscounted() {
        val rules = rules(
            PriceRule(
                validFrom = LocalDate(2020, 1, 1),
                prices = Prices(
                    breakfast = 3.0,
                    breakfastNextDay = 4.5,
                    mitraAllowance = allowance222,
                ),
            ),
        )
        val volunteers = listOf(
            booking(
                LocalDate(2026, 8, 5),
                meals = listOf(Meal.Dinner, Meal.BreakfastNextDay),
                sleep = true,
            ),
            booking(LocalDate(2026, 8, 6), meals = listOf(Meal.Breakfast)),
        )

        val result = calculateMonthlyCharge(volunteers, rules, isMitra = true)

        assertEquals(1, result.breakfasts)
        assertEquals(1, result.breakfastsNextDay)
        assertEquals(3.0, result.breakfastsAmount, TOLERANCE)
        assertEquals(4.5, result.breakfastsNextDayAmount, TOLERANCE)
        // dinner and night are inside the mitra allowance; only the breakfasts are charged
        assertEquals(3.0 + 4.5, result.total, TOLERANCE)
    }

    @Test
    fun allowanceDoesNotCarryOverBetweenMonths() {
        val january = emptyList<Volunteer>()
        val february = (1..3).map { day ->
            booking(LocalDate(2026, 2, day), meals = listOf(Meal.Lunch))
        }

        // January's unused allowance must not grow February's: still only 2 free.
        assertEquals(0.0, calculateMonthlyCharge(january, defaultRules, isMitra = true).total, TOLERANCE)
        val result = calculateMonthlyCharge(february, defaultRules, isMitra = true)
        assertEquals(2, result.freeLunchesUsed)
        assertEquals(8.0, result.total, TOLERANCE)
    }

    companion object {
        private const val TOLERANCE = 0.0001
    }
}
