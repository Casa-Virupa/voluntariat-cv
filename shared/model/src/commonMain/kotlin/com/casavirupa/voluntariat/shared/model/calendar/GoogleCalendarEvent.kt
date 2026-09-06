package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

data class GoogleCalendarEvent(
    val id: Long,
    val googleId: String,
    val title: String,
    val description: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val isAllDay: Boolean,
    val available: Boolean,
) {
    // Google Calendar's end is exclusive: an all-day event ending at 00:00 finishes the
    // day before, so a one-day all-day event has lastDay == start.date.
    val lastDay: LocalDate
        get() = if (end.time == LocalTime(0, 0) && end.date > start.date) {
            end.date.minus(1, DateTimeUnit.DAY)
        } else {
            end.date
        }

    val isMultiDay: Boolean
        get() = lastDay > start.date
}

fun GoogleCalendarEvent.isHappeningOn(
    date: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Boolean {
    val dayStart = date.atStartOfDayIn(timeZone)
    val dayEnd = dayStart.plus(1, DateTimeUnit.DAY, timeZone)

    val eventStart = start.toInstant(timeZone)
    val eventEnd = end.toInstant(timeZone)

    return eventStart < dayEnd && eventEnd > dayStart
}
