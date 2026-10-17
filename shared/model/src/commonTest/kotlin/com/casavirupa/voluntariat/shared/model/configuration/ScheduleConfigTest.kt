package com.casavirupa.voluntariat.shared.model.configuration

import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleConfigTest {
    private val sunday = LocalDate(2026, 10, 4)
    private val saturday = LocalDate(2026, 10, 3)
    private val lateAfternoon = TimeRange(LocalTime(16, 30), LocalTime(20, 30))

    @Test
    fun `an on-site sunday shift ending after the limit is late`() {
        assertTrue(ScheduleConfig.Default.isLateOnSunday(sunday, lateAfternoon, online = false))
    }

    @Test
    fun `ending exactly at the limit is fine`() {
        assertFalse(ScheduleConfig.Default.isLateOnSunday(sunday, TimeRange.SundayAfternoon, online = false))
    }

    @Test
    fun `other days and online shifts are never late`() {
        assertFalse(ScheduleConfig.Default.isLateOnSunday(saturday, lateAfternoon, online = false))
        assertFalse(ScheduleConfig.Default.isLateOnSunday(sunday, lateAfternoon, online = true))
        assertFalse(ScheduleConfig.Default.isLateOnSunday(null, lateAfternoon, online = false))
    }

    @Test
    fun `the limit comes from the config`() {
        val config = ScheduleConfig(sundayEndTime = LocalTime(19, 0))
        assertTrue(config.isLateOnSunday(sunday, TimeRange.SundayAfternoon, online = false))
    }
}
