package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ShiftMinutesTest {
    @Test
    fun `default shifts count their full length`() {
        assertEquals(240, afternoon(TimeRange.DefaultAfternoon).minutes())
        assertEquals(240, morning(TimeRange.DefaultMorning).minutes())
        assertEquals(300, morning(TimeRange.SundayMorning).minutes())
        assertEquals(180, afternoon(TimeRange.SundayAfternoon).minutes())
    }

    @Test
    fun `shortened shifts count only the booked time`() {
        assertEquals(120, afternoon(range(17, 30, 19, 30)).minutes())
        assertEquals(165, afternoon(range(16, 30, 19, 15)).minutes())
    }

    @Test
    fun `empty or implausible ranges count as zero`() {
        assertEquals(0, afternoon(range(16, 30, 16, 30)).minutes())
        assertEquals(0, morning(range(0, 0, 23, 0)).minutes())
    }

    // Pere's October 2026: three shortened afternoons are 8 h, not 3 × 4 h.
    @Test
    fun `a volunteer's total adds up the real shift lengths`() {
        val bookings = listOf(
            volunteer(afternoon(range(16, 30, 19, 30), VolunteerType.Specific(SpecificArea.ExteriorsAndGardening))),
            volunteer(afternoon(range(17, 30, 19, 30))),
            volunteer(afternoon(range(16, 30, 19, 30))),
        )
        assertEquals(480, bookings.sumOf { it.calculateMinutes() })
    }

    private fun range(startH: Int, startM: Int, endH: Int, endM: Int) =
        TimeRange(LocalTime(startH, startM), LocalTime(endH, endM))

    private fun morning(range: TimeRange) = Shift.Morning(VolunteerType.General, range)

    private fun afternoon(range: TimeRange, type: VolunteerType = VolunteerType.General) =
        Shift.Afternoon(type, range)

    private fun volunteer(vararg shifts: Shift) = Volunteer(
        id = VolunteerId("v"),
        userId = UserId("pere"),
        date = LocalDate(2026, 10, 21),
        shifts = shifts.toList(),
        meals = emptyList(),
        sleep = false,
    )
}
