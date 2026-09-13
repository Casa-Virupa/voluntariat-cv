package com.casavirupa.voluntariat.features.calendar.models

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number

@Immutable
data class YearMonth(
    val year: Int,
    val month: Month
) {
    val firstDayOfMonth = LocalDate(year, month, 1)

    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal

    val daysInMonth = month.lengthOfMonth(year.isLeap())

    val lastDayOfMonth = LocalDate(year, month, daysInMonth)


    val nextMonth = if (month.number == 12) Month(1) else Month(month.number + 1)

    val nextYear = if (month.number == 12) year + 1 else year

    val prevMonth = if (month.number == 1) Month(12) else Month(month.number - 1)

    val prevYear = if (month.number == 1) year - 1 else year

    val daysInPrevMonth = prevMonth.lengthOfMonth(prevYear.isLeap())

    /**
     * Months elapsed since January of year 0: an absolute, gap-free index, so consecutive months
     * always differ by exactly 1 (used to map pager pages to months and back).
     */
    val monthIndex: Int get() = year * MONTHS_PER_YEAR + (month.number - 1)

    companion object {
        fun fromMonthIndex(index: Int) = YearMonth(
            year = index.floorDiv(MONTHS_PER_YEAR),
            month = Month(index.mod(MONTHS_PER_YEAR) + 1),
        )
    }
}

private const val MONTHS_PER_YEAR = 12

internal fun Month.lengthOfMonth(isLeap: Boolean): Int =
    when (this) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (isLeap) 29 else 28
    }

internal fun Int.isLeap(): Boolean =
    (this % 4 == 0 && this % 100 != 0) || (this % 400 == 0)
