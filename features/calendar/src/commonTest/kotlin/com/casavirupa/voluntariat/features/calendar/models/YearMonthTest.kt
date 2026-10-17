package com.casavirupa.voluntariat.features.calendar.models

import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class YearMonthTest {
    @Test
    fun monthIndexRoundTrips() {
        val months = listOf(
            YearMonth(2000, Month.JANUARY),
            YearMonth(2026, Month.SEPTEMBER),
            YearMonth(2026, Month.DECEMBER),
            YearMonth(2027, Month.JANUARY),
            YearMonth(2099, Month.DECEMBER),
        )
        months.forEach { month ->
            assertEquals(month, YearMonth.fromMonthIndex(month.monthIndex))
        }
    }

    @Test
    fun consecutiveMonthsDifferByOne() {
        val december = YearMonth(2026, Month.DECEMBER)
        val january = YearMonth(2027, Month.JANUARY)
        assertEquals(december.monthIndex + 1, january.monthIndex)
    }

    @Test
    fun monthIndexIsConsistentWithNextAndPrevMonth() {
        var month = YearMonth(2024, Month.JANUARY)
        repeat(36) {
            val next = YearMonth(month.nextYear, month.nextMonth)
            assertEquals(next, YearMonth.fromMonthIndex(month.monthIndex + 1))
            assertEquals(month, YearMonth.fromMonthIndex(next.monthIndex - 1))
            assertEquals(YearMonth(next.prevYear, next.prevMonth), month)
            month = next
        }
    }
}
