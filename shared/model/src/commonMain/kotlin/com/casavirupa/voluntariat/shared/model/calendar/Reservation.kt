package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.LocalDate

data class Reservation(
    val id: ReservationId,
    val date: LocalDate,
    val volunteerShift: VolunteerShift,
    val technicalAreaTurn: TechnicalAreaTurn,
    val meal: Meal,
    val sleep: Boolean,
)

data class ReservationId(val value: String)

enum class VolunteerShift {
    Morning,
    Afternoon,
    AllDay,
}

enum class TechnicalAreaTurn {
    Morning,
    Afternoon,
    AllDay,
}

enum class Meal {
    Lunch,
    Dinner,
}
