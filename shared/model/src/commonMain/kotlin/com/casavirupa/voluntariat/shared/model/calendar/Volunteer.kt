package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Volunteer(
    val id: VolunteerId,
    val userId: UserId,
    val date: LocalDate,
    val shifts: List<Shift>,
    val meals: List<Meal>,
    val sleep: Boolean,
) {
    fun calculateTotalToPay() = 2.0
}

data class VolunteerId(val value: String) {
    companion object {
        val Empty = VolunteerId("")
    }
}

sealed class Shift {
    abstract val type: VolunteerType
    abstract val timeRange: TimeRange

    data class Morning(
        override val type: VolunteerType,
        override val timeRange: TimeRange,
    ) : Shift()

    data class Afternoon(
        override val type: VolunteerType,
        override val timeRange: TimeRange,
    ) : Shift()
}

data class TimeRange(
    val start: LocalTime,
    val end: LocalTime,
) {
    companion object {
        val DefaultMorning = TimeRange(
            start = LocalTime(10, 0),
            end = LocalTime(14, 0)
        )
        val DefaultAfternoon = TimeRange(
            start = LocalTime(16, 30),
            end = LocalTime(20, 30),
        )
    }
}

enum class Meal {
    Lunch,
    Dinner,
    Unknown,
}

sealed class VolunteerType {
    data object General : VolunteerType()

    data class Specific(val specificArea: SpecificArea) : VolunteerType()
}
