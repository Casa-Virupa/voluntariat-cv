package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.User

// Month-view filter. Only one is active at a time; none active means "everything visible".
enum class CalendarFilter {
    General,
    Specific,
    Mine;

    fun matches(volunteer: Volunteer, viewer: User): Boolean =
        when (this) {
            General -> volunteer.shifts.any { it.type is VolunteerType.General }
            Specific -> volunteer.shifts.any { it.type is VolunteerType.Specific }
            Mine -> volunteer.isOwnedBy(viewer)
        }
}
