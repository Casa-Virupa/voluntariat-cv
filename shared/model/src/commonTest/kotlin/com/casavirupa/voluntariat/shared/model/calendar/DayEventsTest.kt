package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class DayEventsTest {
    private val timeZone = TimeZone.of("Europe/Madrid")

    private fun event(
        start: LocalDateTime,
        end: LocalDateTime,
        isAllDay: Boolean = false,
        available: Boolean = true,
    ) = GoogleCalendarEvent(
        id = 0,
        googleId = "g",
        title = if (available) "Xerrada" else "NO VOLUNTARIAT",
        description = "",
        start = start,
        end = end,
        isAllDay = isAllDay,
        available = available,
    )

    private val events = listOf(
        // One-day all-day event: Google's exclusive end lands on the next day at 00:00.
        event(
            start = LocalDateTime(2026, 9, 14, 0, 0),
            end = LocalDateTime(2026, 9, 15, 0, 0),
            isAllDay = true,
            available = false,
        ),
        // Three-day all-day block.
        event(
            start = LocalDateTime(2026, 10, 1, 0, 0),
            end = LocalDateTime(2026, 10, 4, 0, 0),
            isAllDay = true,
            available = false,
        ),
        // Timed morning block.
        event(
            start = LocalDateTime(2026, 9, 20, 9, 0),
            end = LocalDateTime(2026, 9, 20, 13, 0),
            available = false,
        ),
        // Timed event crossing midnight (ends after 00:00, so it touches the next day too).
        event(
            start = LocalDateTime(2026, 9, 25, 20, 0),
            end = LocalDateTime(2026, 9, 26, 1, 0),
        ),
        // Timed event ending exactly at midnight: does not touch the next day.
        event(
            start = LocalDateTime(2026, 9, 27, 20, 0),
            end = LocalDateTime(2026, 9, 28, 0, 0),
        ),
        // Across the DST change (25 Oct 2026).
        event(
            start = LocalDateTime(2026, 10, 24, 10, 0),
            end = LocalDateTime(2026, 10, 26, 10, 0),
            available = false,
        ),
    )

    @Test
    fun groupByDayMatchesIsHappeningOnForEveryDay() {
        val byDay = events.groupByDay(timeZone)
        var day = LocalDate(2026, 8, 25)
        val last = LocalDate(2026, 11, 5)
        while (day <= last) {
            val expected = events.filter { it.isHappeningOn(day, timeZone) }
            assertEquals(expected, byDay[day]?.events ?: emptyList(), "events on $day")
            assertEquals(
                expected.unavailabilityOn(day, timeZone),
                byDay[day]?.unavailability ?: DayCoverage.None,
                "unavailability on $day",
            )
            day = day.plus(1, DateTimeUnit.DAY)
        }
    }

    @Test
    fun coverageIsComputedPerDay() {
        val byDay = events.groupByDay(timeZone)
        assertEquals(DayCoverage.WholeDay, byDay[LocalDate(2026, 9, 14)]?.unavailability)
        assertEquals(DayCoverage.Morning, byDay[LocalDate(2026, 9, 20)]?.unavailability)
        assertEquals(DayCoverage.None, byDay[LocalDate(2026, 9, 25)]?.unavailability)
        assertEquals(null, byDay[LocalDate(2026, 9, 28)])
        assertEquals(DayCoverage.WholeDay, byDay[LocalDate(2026, 10, 25)]?.unavailability)
        assertEquals(DayCoverage.Morning, byDay[LocalDate(2026, 10, 26)]?.unavailability)
    }
}
