package com.casavirupa.voluntariat.features.calendar.models

import androidx.compose.runtime.Immutable
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlin.time.Duration.Companion.days

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