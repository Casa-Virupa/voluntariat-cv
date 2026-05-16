package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate

data class Volunteer(
    val id: VolunteerId,
    val userId: UserId,
    val date: LocalDate,
    val shift: Shift,
    val specificArea: SpecificArea?,
    val meals: List<Meal>,
    val sleep: Boolean,
)

data class VolunteerId(val value: String) {
    companion object {
        val Empty = VolunteerId("")
    }
}

enum class Shift {
    Morning,
    Afternoon,
    AllDay,
    Unknown,
}

enum class SpecificArea {
    Morning,
    Afternoon,
    AllDay,
}

enum class Meal {
    Lunch,
    Dinner,
    Unknown,
}

enum class VolunteerType {
    General,
    Specific
}
