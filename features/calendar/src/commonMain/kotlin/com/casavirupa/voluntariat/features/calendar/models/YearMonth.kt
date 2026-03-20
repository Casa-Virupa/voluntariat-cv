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

    val nextMonth = Month(month.number + 1)

    val nextYear = if (nextMonth.number == 1) year + 1 else year

    val prevMonth = Month(month.number - 1)

    val prevYear = if (prevMonth.number == 12) year - 1 else year
}

internal fun Month.lengthOfMonth(isLeap: Boolean): Int =
    when (this) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (isLeap) 29 else 28
    }

internal fun Int.isLeap(): Boolean =
    (this % 4 == 0 && this % 100 != 0) || (this % 400 == 0)