package com.casavirupa.voluntariat.shared.model.calendar

data class Reservation(
    val id: ReservationId
)

data class ReservationId(val value: String)
