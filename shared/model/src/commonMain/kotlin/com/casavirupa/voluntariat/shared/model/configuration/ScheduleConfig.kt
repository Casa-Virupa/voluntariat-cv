package com.casavirupa.voluntariat.shared.model.configuration

import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * Mirror of the `configuration/schedule` document (set by hand in the Firebase console for now).
 *
 * [sundayEndTime] is when on-site volunteering ends on Sundays (issue #105). When the document
 * is missing, malformed or can't be read the repository emits [Default], today's 19:30.
 */
data class ScheduleConfig(
    val sundayEndTime: LocalTime = DefaultSundayEndTime,
) {
    /**
     * True when an on-site shift on a Sunday ends after [sundayEndTime]; ending exactly then is
     * fine. Online shifts aren't bound by the house's hours. It's only a warning: the booking
     * can still be saved.
     */
    fun isLateOnSunday(date: LocalDate?, timeRange: TimeRange, online: Boolean): Boolean =
        date?.dayOfWeek == DayOfWeek.SUNDAY && !online && timeRange.end > sundayEndTime

    companion object {
        val DefaultSundayEndTime = LocalTime(19, 30)
        val Default = ScheduleConfig()
    }
}
