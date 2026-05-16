package com.casavirupa.voluntariat.shared.model.calendar

import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class Volunteer(
    val id: VolunteerId,
    val userId: UserId,
    val date: LocalDate,
    val shift: Shift,
    val meals: List<Meal>,
    val sleep: Boolean,
)

data class VolunteerId(val value: String) {
    companion object {
        val Empty = VolunteerId("")
    }
}

sealed class Shift {
    data class Morning(
        val type: VolunteerType,
        val timeRange: TimeRange,
    ) : Shift()

    data class Afternoon(
        val type: VolunteerType,
        val timeRange: TimeRange,
    ) : Shift()

    data class AllDay(
        val morningType: VolunteerType,
        val afternoonType: VolunteerType,
        val morningTimeRange: TimeRange,
        val afternoonTimeRange: TimeRange,
    ) : Shift()

    data object Unknown : Shift()
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

enum class VolunteerType {
    General,
    Specific
}
