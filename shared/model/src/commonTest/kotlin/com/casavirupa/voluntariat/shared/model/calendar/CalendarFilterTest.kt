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

class CalendarFilterTest {
    private val viewer = User(
        id = UserId("viewer"),
        name = "Viewer",
        email = "viewer@example.com",
        role = UserRole.Volunteer(UserVolunteerType.Habitual),
        volunteerType = UserVolunteerType.Habitual,
        hasOnboardingCompleted = true,
        specificAreas = emptyList(),
        isMember = false,
    )
    private val general = Shift.Morning(VolunteerType.General, TimeRange.DefaultMorning)
    private val specific = Shift.Afternoon(
        VolunteerType.Specific(SpecificArea.Kitchen),
        TimeRange.DefaultAfternoon,
    )

    @Test
    fun `general filter keeps volunteers with at least one general shift`() {
        assertTrue(CalendarFilter.General.matches(volunteer("other", general, specific), viewer))
        assertFalse(CalendarFilter.General.matches(volunteer("other", specific), viewer))
    }

    @Test
    fun `specific filter keeps volunteers with at least one specific shift`() {
        assertTrue(CalendarFilter.Specific.matches(volunteer("other", specific), viewer))
        assertFalse(CalendarFilter.Specific.matches(volunteer("other", general), viewer))
    }

    @Test
    fun `mine filter keeps only the viewer's own volunteering`() {
        assertTrue(CalendarFilter.Mine.matches(volunteer("viewer", specific), viewer))
        assertFalse(CalendarFilter.Mine.matches(volunteer("other", general), viewer))
    }

    private fun volunteer(userId: String, vararg shifts: Shift) =
        Volunteer(
            id = VolunteerId("v-$userId"),
            userId = UserId(userId),
            date = LocalDate(2026, 9, 12),
            shifts = shifts.toList(),
            meals = emptyList(),
            sleep = false,
        )
}
