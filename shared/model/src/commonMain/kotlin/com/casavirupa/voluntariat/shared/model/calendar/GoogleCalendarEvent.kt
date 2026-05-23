package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
)

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
