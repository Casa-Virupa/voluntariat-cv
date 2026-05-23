package com.casavirupa.voluntariart.shared.data.repositories.responses

import kotlinx.serialization.Serializable

@Serializable
data class GoogleCalendarEventResponse(
    val id: String,
    val title: String,
    val description: String,
    val start: String,
    val end: String,
    val isAllDay: Boolean,
)
