package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/** The Google Calendar events of one day and the «NO VOLUNTARIAT» coverage they add up to. */
data class DayEvents(
    val events: List<GoogleCalendarEvent>,
    val unavailability: DayCoverage,
)

/**
 * Indexes the events by every day they touch, from [GoogleCalendarEvent.start]'s date to
 * [GoogleCalendarEvent.lastDay] — the same days for which [isHappeningOn] holds, computed once
 * per event with plain date arithmetic instead of per cell with instant conversions.
 */
fun List<GoogleCalendarEvent>.groupByDay(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Map<LocalDate, DayEvents> {
    val eventsByDay = mutableMapOf<LocalDate, MutableList<GoogleCalendarEvent>>()
    forEach { event ->
        var day = event.start.date
        val lastDay = event.lastDay
        while (day <= lastDay) {
            eventsByDay.getOrPut(day) { mutableListOf() }.add(event)
            day = day.plus(1, DateTimeUnit.DAY)
        }
    }
    return eventsByDay.mapValues { (day, events) ->
        DayEvents(events = events, unavailability = events.unavailabilityOn(day, timeZone))
    }
}
