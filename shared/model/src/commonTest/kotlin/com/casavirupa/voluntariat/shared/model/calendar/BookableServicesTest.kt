package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BookableServicesTest {
    private val mitra = user(UserVolunteerType.Mitra)
    private val habitual = user(UserVolunteerType.Habitual)

    @Test
    fun `mitra can book everything without volunteering`() {
        assertEquals(BookableServices.All, mitra.bookableServices(morning = false, afternoon = false, nextDay = null))
    }

    @Test
    fun `habitual without volunteering can book nothing`() {
        assertEquals(nothing, habitual.bookableServices(morning = false, afternoon = false, nextDay = null))
    }

    @Test
    fun `morning shift allows breakfast and lunch`() {
        assertEquals(
            BookableServices(setOf(Meal.Breakfast, Meal.Lunch), sleep = false),
            habitual.bookableServices(morning = true, afternoon = false, nextDay = null),
        )
    }

    @Test
    fun `afternoon shift allows lunch dinner the night and next day's breakfast`() {
        assertEquals(
            BookableServices(setOf(Meal.Lunch, Meal.Dinner, Meal.BreakfastNextDay), sleep = true),
            habitual.bookableServices(morning = false, afternoon = true, nextDay = null),
        )
    }

    @Test
    fun `both shifts allow everything`() {
        assertEquals(BookableServices.All, habitual.bookableServices(morning = true, afternoon = true, nextDay = null))
    }

    @Test
    fun `an on-site morning shift the next day allows sleeping the night before`() {
        assertEquals(
            BookableServices(setOf(Meal.BreakfastNextDay), sleep = true),
            habitual.bookableServices(morning = false, afternoon = false, nextDay = booking(morningShift(online = false))),
        )
    }

    @Test
    fun `an online morning shift the next day allows nothing`() {
        assertEquals(
            nothing,
            habitual.bookableServices(morning = false, afternoon = false, nextDay = booking(morningShift(online = true))),
        )
    }

    @Test
    fun `an afternoon shift the next day doesn't allow the night before`() {
        val afternoon = Shift.Afternoon(VolunteerType.General, TimeRange.DefaultAfternoon)
        assertEquals(nothing, habitual.bookableServices(morning = false, afternoon = false, nextDay = booking(afternoon)))
    }

    @Test
    fun `no next-day breakfast when the next day's booking already has breakfast`() {
        val nextDay = booking(morningShift(online = false), meals = listOf(Meal.Breakfast))
        assertEquals(
            BookableServices(emptySet(), sleep = true),
            habitual.bookableServices(morning = false, afternoon = false, nextDay = nextDay),
        )
        assertEquals(
            BookableServices(setOf(Meal.Lunch, Meal.Dinner), sleep = true),
            habitual.bookableServices(morning = false, afternoon = true, nextDay = nextDay),
        )
    }

    @Test
    fun `a user without volunteer type is restricted like a habitual`() {
        assertEquals(nothing, user(null).bookableServices(morning = false, afternoon = false, nextDay = null))
    }

    private val nothing = BookableServices(emptySet(), sleep = false)

    private fun morningShift(online: Boolean) =
        Shift.Morning(VolunteerType.General, TimeRange.DefaultMorning, online = online)

    private fun booking(vararg shifts: Shift, meals: List<Meal> = emptyList()) =
        Volunteer(
            id = VolunteerId("next"),
            userId = UserId("u"),
            date = LocalDate(2026, 10, 2),
            shifts = shifts.toList(),
            meals = meals,
            sleep = false,
        )

    private fun user(type: UserVolunteerType?) = User(
        id = UserId("u"),
        name = "u",
        email = "u@example.com",
        role = type?.let(UserRole::Volunteer) ?: UserRole.Unknown,
        volunteerType = type,
        hasOnboardingCompleted = true,
        specificAreas = emptyList(),
        isMember = true,
    )
}
