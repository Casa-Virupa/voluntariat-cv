package com.casavirupa.voluntariat.features.calendar.utils

import com.casavirupa.voluntariat.features.calendar.models.isLeap
import com.casavirupa.voluntariat.features.calendar.models.lengthOfMonth
import kotlinx.datetime.Month
import kotlinx.datetime.number

internal data class MonthCalculations(
    val month: Month,
    val year: Int,
) {
    val prevMonth = if (month.number == 1) Month(12) else Month(month.number - 1)

    val prevYear = if (month.number == 1) year - 1 else year

    val daysInPrevMonth = prevMonth.lengthOfMonth(prevYear.isLeap())

    val nextMonth: Month = if (month.number == 12) Month(1) else Month(month.number + 1)

    val nextYear = if (month.number == 12) year + 1 else year
}