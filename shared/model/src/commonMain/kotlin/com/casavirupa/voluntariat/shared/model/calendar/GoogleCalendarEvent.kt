package com.casavirupa.voluntariat.shared.model.calendar

import kotlinx.datetime.LocalDateTime

data class GoogleCalendarEvent(
    val id: Long,
    val googleId: String,
    val title: String,
    val description: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
)
