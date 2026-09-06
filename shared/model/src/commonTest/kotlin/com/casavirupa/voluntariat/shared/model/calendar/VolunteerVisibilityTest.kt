package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VolunteerVisibilityTest {
    private val viewer = user(id = "viewer", areas = listOf(SpecificArea.Kitchen))
    private val coordinator = user(id = "coord", role = UserRole.CoordinationTeam)

    @Test
    fun `own volunteering is always visible with its services`() {
        val mine = volunteer(userId = "viewer", Shift.Morning(specific(SpecificArea.Grove), TimeRange.DefaultMorning))
        assertTrue(mine.isVisibleTo(viewer))
        assertTrue(mine.areServicesVisibleTo(viewer))
    }

    @Test
    fun `general volunteering of others is visible but its services are not`() {
        val other = volunteer(userId = "other", Shift.Morning(VolunteerType.General, TimeRange.DefaultMorning))
        assertTrue(other.isVisibleTo(viewer))
        assertFalse(other.areServicesVisibleTo(viewer))
    }

    @Test
    fun `specific volunteering in one of the viewer's areas is visible`() {
        val other = volunteer(userId = "other", Shift.Afternoon(specific(SpecificArea.Kitchen), TimeRange.DefaultAfternoon))
        assertTrue(other.isVisibleTo(viewer))
    }

    @Test
    fun `specific volunteering in a foreign area is hidden`() {
        val other = volunteer(userId = "other", Shift.Afternoon(specific(SpecificArea.Grove), TimeRange.DefaultAfternoon))
        assertFalse(other.isVisibleTo(viewer))
    }

    @Test
    fun `mixed day is visible when at least one shift matches`() {
        val other = volunteer(
            userId = "other",
            Shift.Morning(specific(SpecificArea.Grove), TimeRange.DefaultMorning),
            Shift.Afternoon(VolunteerType.General, TimeRange.DefaultAfternoon),
        )
        assertTrue(other.isVisibleTo(viewer))
    }

    @Test
    fun `coordination team sees everyone and their services`() {
        val other = volunteer(userId = "other", Shift.Afternoon(specific(SpecificArea.Grove), TimeRange.DefaultAfternoon))
        assertTrue(other.isVisibleTo(coordinator))
        assertTrue(other.areServicesVisibleTo(coordinator))
    }

    private fun specific(area: SpecificArea) = VolunteerType.Specific(area)

    private fun volunteer(userId: String, vararg shifts: Shift) =
        Volunteer(
            id = VolunteerId("v-$userId"),
            userId = UserId(userId),
            date = LocalDate(2026, 9, 12),
            shifts = shifts.toList(),
            meals = listOf(Meal.Lunch),
            sleep = true,
        )

    private fun user(
        id: String,
        role: UserRole = UserRole.Volunteer(UserVolunteerType.Habitual),
        areas: List<SpecificArea> = emptyList(),
    ) = User(
        id = UserId(id),
        name = id,
        email = "$id@example.com",
        role = role,
        volunteerType = UserVolunteerType.Habitual,
        hasOnboardingCompleted = true,
        specificAreas = areas,
        isMember = false,
    )
}
