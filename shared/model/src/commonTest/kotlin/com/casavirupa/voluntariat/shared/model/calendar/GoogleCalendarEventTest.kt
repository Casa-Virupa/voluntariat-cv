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
