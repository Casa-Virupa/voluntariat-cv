package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DayCardServicesTest {
    private val allMeals = listOf(Meal.Breakfast, Meal.Lunch, Meal.Dinner, Meal.BreakfastNextDay)

    @Test
    fun `morning card shows breakfast and lunch and afternoon card dinner and the night`() {
        val booking = booking(morning(), afternoon())
        assertEquals(CardServices(listOf(Meal.Breakfast, Meal.Lunch), sleep = false), booking.servicesOnCard(0))
        assertEquals(CardServices(listOf(Meal.Dinner, Meal.BreakfastNextDay), sleep = true), booking.servicesOnCard(1))
    }

    @Test
    fun `afternoon-only card shows lunch too`() {
        val booking = booking(afternoon(), meals = listOf(Meal.Lunch, Meal.Dinner))
        assertEquals(CardServices(listOf(Meal.Lunch, Meal.Dinner), sleep = true), booking.servicesOnCard(0))
    }

    @Test
    fun `afternoon-only card keeps a same-day breakfast`() {
        val booking = booking(afternoon(), meals = listOf(Meal.Breakfast), sleep = false)
        assertEquals(CardServices(listOf(Meal.Breakfast), sleep = false), booking.servicesOnCard(0))
    }

    @Test
    fun `morning-only card keeps the night`() {
        val booking = booking(morning())
        assertEquals(CardServices(allMeals, sleep = true), booking.servicesOnCard(0))
    }

    // Temple then Transcripcions (issue #113): services only on the first afternoon card
    @Test
    fun `two afternoon cards don't repeat the services`() {
        val booking = booking(afternoon(18, 30, 20, 30), afternoon(16, 30, 18, 30))
        assertEquals(CardServices(emptyList(), sleep = false), booking.servicesOnCard(0))
        assertEquals(CardServices(allMeals, sleep = true), booking.servicesOnCard(1))
    }

    @Test
    fun `morning and two afternoons split the services once`() {
        val booking = booking(morning(), afternoon(16, 30, 18, 30), afternoon(18, 30, 20, 30))
        assertEquals(CardServices(listOf(Meal.Breakfast, Meal.Lunch), sleep = false), booking.servicesOnCard(0))
        assertEquals(CardServices(listOf(Meal.Dinner, Meal.BreakfastNextDay), sleep = true), booking.servicesOnCard(1))
        assertEquals(CardServices(emptyList(), sleep = false), booking.servicesOnCard(2))
    }

    private fun morning() = Shift.Morning(VolunteerType.General, TimeRange.DefaultMorning)

    private fun afternoon(startH: Int = 16, startM: Int = 30, endH: Int = 20, endM: Int = 30) =
        Shift.Afternoon(VolunteerType.General, TimeRange(LocalTime(startH, startM), LocalTime(endH, endM)))

    private fun booking(
        vararg shifts: Shift,
        meals: List<Meal> = allMeals,
        sleep: Boolean = true,
    ) = Volunteer(
        id = VolunteerId("v"),
        userId = UserId("laura"),
        date = LocalDate(2026, 10, 7),
        shifts = shifts.toList(),
        meals = meals,
        sleep = sleep,
    )
}
