package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface CalendarRepository {
    suspend fun syncGoogleCalendarEvents(
        year: Int,
        startMonth: Int,
        endMonth: Int,
    ): Result<Unit>

    fun getGoogleCalendarEvents(): Flow<List<GoogleCalendarEvent>>
}