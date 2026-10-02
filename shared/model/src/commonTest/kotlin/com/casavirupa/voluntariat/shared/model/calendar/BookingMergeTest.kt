package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookingMergeTest {
    private val temple = afternoon(16, 30, 18, 30)
    private val transcriptions =
        afternoon(18, 30, 20, 30, VolunteerType.Specific(SpecificArea.ExteriorsAndGardening))

    // Laura books Temple, then comes back to add a second afternoon shift (issue #113)
    @Test
    fun `a back-to-back shift on a booked day doesn't overlap`() {
        assertNull(booking(temple).firstOverlapWith(listOf(transcriptions)))
    }

    @Test
    fun `a shift overlapping a booked one is reported`() {
        val overlapping = afternoon(18, 0, 20, 0)
        assertEquals(temple, booking(temple).firstOverlapWith(listOf(overlapping)))
    }

    @Test
    fun `merging adds the shifts in start order and keeps the booking id`() {
        val merged = booking(transcriptions, id = "old").mergedWith(booking(temple, id = ""))
        assertEquals(VolunteerId("old"), merged.id)
        assertEquals(listOf(temple, transcriptions), merged.shifts)
    }

    @Test
    fun `merging joins meals without duplicates and keeps the night`() {
        val booked = booking(temple, meals = listOf(Meal.Lunch, Meal.Dinner), sleep = true)
        val addition = booking(transcriptions, meals = listOf(Meal.Dinner, Meal.BreakfastNextDay))
        val merged = booked.mergedWith(addition)
        assertEquals(listOf(Meal.Lunch, Meal.Dinner, Meal.BreakfastNextDay), merged.meals)
        assertEquals(true, merged.sleep)
    }

    private fun afternoon(
        startH: Int,
        startM: Int,
        endH: Int,
        endM: Int,
        type: VolunteerType = VolunteerType.General,
    ) = Shift.Afternoon(type, TimeRange(LocalTime(startH, startM), LocalTime(endH, endM)))

    private fun booking(
        vararg shifts: Shift,
        id: String = "v",
        meals: List<Meal> = emptyList(),
        sleep: Boolean = false,
    ) = Volunteer(
        id = VolunteerId(id),
        userId = UserId("laura"),
        date = LocalDate(2026, 10, 16),
        shifts = shifts.toList(),
        meals = meals,
        sleep = sleep,
    )
}
