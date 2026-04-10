package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.LocalDate

data class Reservation(
    val id: ReservationId,
    val date: LocalDate,
    val scheduleRange: ScheduleRange,
    val technicalAreaTurn: TechnicalAreaTurn,
    val mealType: MealType,
    val sleep: Boolean,
)

data class ReservationId(val value: String)

enum class ScheduleRange {
    Morning,
    Afternoon,
    AllDay,
}

enum class TechnicalAreaTurn {
    Morning,
    Afternoon,
    AllDay,
}

enum class MealType {
    Breakfast,
    Lunch,
    Dinner,
}
