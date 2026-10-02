package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShiftOverlapTest {
    // Temple then Transcripcions in one afternoon (issue #113)
    @Test
    fun `back-to-back shifts don't overlap`() {
        assertFalse(range(16, 30, 18, 30).overlaps(range(18, 30, 20, 30)))
        assertFalse(range(18, 30, 20, 30).overlaps(range(16, 30, 18, 30)))
    }

    @Test
    fun `partially overlapping shifts overlap`() {
        assertTrue(range(16, 30, 18, 30).overlaps(range(18, 0, 20, 0)))
        assertTrue(range(18, 0, 20, 0).overlaps(range(16, 30, 18, 30)))
    }

    @Test
    fun `a shift inside another overlaps it`() {
        assertTrue(range(16, 30, 20, 30).overlaps(range(17, 0, 18, 0)))
        assertTrue(range(17, 0, 18, 0).overlaps(range(16, 30, 20, 30)))
    }

    @Test
    fun `identical shifts overlap`() {
        assertTrue(TimeRange.DefaultAfternoon.overlaps(TimeRange.DefaultAfternoon))
    }

    @Test
    fun `default morning and afternoon don't overlap`() {
        assertFalse(TimeRange.DefaultMorning.overlaps(TimeRange.DefaultAfternoon))
        assertFalse(TimeRange.SundayMorning.overlaps(TimeRange.SundayAfternoon))
    }

    @Test
    fun `a morning shift running into the afternoon overlaps it`() {
        assertTrue(range(10, 0, 17, 0).overlaps(TimeRange.DefaultAfternoon))
    }

    @Test
    fun `empty or inverted ranges overlap nothing`() {
        assertFalse(range(17, 0, 17, 0).overlaps(TimeRange.DefaultAfternoon))
        assertFalse(TimeRange.DefaultAfternoon.overlaps(range(19, 0, 18, 0)))
    }

    private fun range(startH: Int, startM: Int, endH: Int, endM: Int) =
        TimeRange(LocalTime(startH, startM), LocalTime(endH, endM))
}
