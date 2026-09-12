package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoogleCalendarEventTest {
    @Test
    fun `timed event within one day is not multi-day`() {
        val event = event(
            start = LocalDateTime(2026, 9, 12, 10, 0),
            end = LocalDateTime(2026, 9, 12, 14, 0),
        )
        assertFalse(event.isMultiDay)
        assertEquals(LocalDate(2026, 9, 12), event.lastDay)
    }

    @Test
    fun `one-day all-day event ending at next midnight is not multi-day`() {
        val event = event(
            start = LocalDateTime(2026, 9, 12, 0, 0),
            end = LocalDateTime(2026, 9, 13, 0, 0),
            isAllDay = true,
        )
        assertFalse(event.isMultiDay)
        assertEquals(LocalDate(2026, 9, 12), event.lastDay)
    }

    @Test
    fun `all-day event spanning three days ends the day before its exclusive end`() {
        val event = event(
            start = LocalDateTime(2026, 9, 12, 0, 0),
            end = LocalDateTime(2026, 9, 15, 0, 0),
            isAllDay = true,
        )
        assertTrue(event.isMultiDay)
        assertEquals(LocalDate(2026, 9, 14), event.lastDay)
    }

    @Test
    fun `timed event finishing on a later day is multi-day`() {
        val event = event(
            start = LocalDateTime(2026, 9, 12, 18, 0),
            end = LocalDateTime(2026, 9, 14, 13, 0),
        )
        assertTrue(event.isMultiDay)
        assertEquals(LocalDate(2026, 9, 14), event.lastDay)
    }

    private fun event(
        start: LocalDateTime,
        end: LocalDateTime,
        isAllDay: Boolean = false,
    ) = GoogleCalendarEvent(
        id = 1L,
        googleId = "g1",
        title = "Ensenyances intro budisme",
        description = "",
        start = start,
        end = end,
        isAllDay = isAllDay,
        available = true,
    )
}

class DayCoverageTest {
    private val day = LocalDate(2026, 9, 12)
    private val tz = kotlinx.datetime.TimeZone.UTC

    @Test
    fun `all-day block covers the whole day`() {
        val event = blocked(LocalDateTime(2026, 9, 12, 0, 0), LocalDateTime(2026, 9, 13, 0, 0), isAllDay = true)
        assertEquals(DayCoverage.WholeDay, event.coverageOn(day, tz))
    }

    @Test
    fun `block ending before 14h covers only the morning`() {
        val event = blocked(LocalDateTime(2026, 9, 12, 9, 0), LocalDateTime(2026, 9, 12, 13, 0))
        assertEquals(DayCoverage.Morning, event.coverageOn(day, tz))
    }

    @Test
    fun `block starting at 14h or later covers only the afternoon`() {
        val event = blocked(LocalDateTime(2026, 9, 12, 14, 0), LocalDateTime(2026, 9, 12, 20, 0))
        assertEquals(DayCoverage.Afternoon, event.coverageOn(day, tz))
    }

    @Test
    fun `timed block spanning 14h covers the whole day`() {
        val event = blocked(LocalDateTime(2026, 9, 12, 10, 0), LocalDateTime(2026, 9, 12, 18, 0))
        assertEquals(DayCoverage.WholeDay, event.coverageOn(day, tz))
    }

    @Test
    fun `multi-day timed block is clipped to the viewed day`() {
        val event = blocked(LocalDateTime(2026, 9, 11, 18, 0), LocalDateTime(2026, 9, 12, 12, 0))
        assertEquals(DayCoverage.Morning, event.coverageOn(day, tz))
        assertEquals(DayCoverage.Afternoon, event.coverageOn(LocalDate(2026, 9, 11), tz))
        assertEquals(DayCoverage.None, event.coverageOn(LocalDate(2026, 9, 13), tz))
    }

    @Test
    fun `available events never count as unavailability`() {
        val events = listOf(
            blocked(LocalDateTime(2026, 9, 12, 0, 0), LocalDateTime(2026, 9, 13, 0, 0), isAllDay = true)
                .copy(available = true),
        )
        assertEquals(DayCoverage.None, events.unavailabilityOn(day, tz))
    }

    @Test
    fun `morning and afternoon blocks add up to the whole day`() {
        val events = listOf(
            blocked(LocalDateTime(2026, 9, 12, 9, 0), LocalDateTime(2026, 9, 12, 12, 0)),
            blocked(LocalDateTime(2026, 9, 12, 16, 0), LocalDateTime(2026, 9, 12, 19, 0)),
        )
        assertEquals(DayCoverage.WholeDay, events.unavailabilityOn(day, tz))
    }

    @Test
    fun `two morning blocks stay a morning block`() {
        val events = listOf(
            blocked(LocalDateTime(2026, 9, 12, 9, 0), LocalDateTime(2026, 9, 12, 10, 0)),
            blocked(LocalDateTime(2026, 9, 12, 11, 0), LocalDateTime(2026, 9, 12, 12, 0)),
        )
        assertEquals(DayCoverage.Morning, events.unavailabilityOn(day, tz))
    }

    private fun blocked(start: LocalDateTime, end: LocalDateTime, isAllDay: Boolean = false) =
        GoogleCalendarEvent(
            id = 2L,
            googleId = "g2",
            title = "NO VOLUNTARIAT",
            description = "",
            start = start,
            end = end,
            isAllDay = isAllDay,
            available = false,
        )
}
