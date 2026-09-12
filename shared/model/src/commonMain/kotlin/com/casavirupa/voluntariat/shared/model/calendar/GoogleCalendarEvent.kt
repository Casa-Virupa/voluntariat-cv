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

// Which part of a day an event occupies. Morning/afternoon are split at 14:00, the same
// boundary the volunteering shifts use.
enum class DayCoverage {
    None,
    Morning,
    Afternoon,
    WholeDay,
}

fun GoogleCalendarEvent.coverageOn(
    date: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): DayCoverage {
    if (!isHappeningOn(date, timeZone)) return DayCoverage.None
    if (isAllDay) return DayCoverage.WholeDay

    val dayStart = date.atStartOfDayIn(timeZone)
    val dayEnd = dayStart.plus(1, DateTimeUnit.DAY, timeZone)
    val midday = LocalDateTime(date, LocalTime(MIDDAY_HOUR, 0)).toInstant(timeZone)
    val clippedStart = maxOf(start.toInstant(timeZone), dayStart)
    val clippedEnd = minOf(end.toInstant(timeZone), dayEnd)

    val touchesMorning = clippedStart < midday
    val touchesAfternoon = clippedEnd > midday
    return when {
        touchesMorning && touchesAfternoon -> DayCoverage.WholeDay
        touchesMorning -> DayCoverage.Morning
        else -> DayCoverage.Afternoon
    }
}

// Combined "NO VOLUNTARIAT" coverage of a day: an all-day block greys the whole cell,
// a timed one only the half it falls in; morning + afternoon blocks add up to the whole day.
fun List<GoogleCalendarEvent>.unavailabilityOn(
    date: LocalDate,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): DayCoverage =
    filter { !it.available }
        .map { it.coverageOn(date, timeZone) }
        .fold(DayCoverage.None) { acc, coverage ->
            when {
                coverage == DayCoverage.None -> acc
                acc == DayCoverage.None -> coverage
                acc == coverage -> acc
                else -> DayCoverage.WholeDay
            }
        }

private const val MIDDAY_HOUR = 14
