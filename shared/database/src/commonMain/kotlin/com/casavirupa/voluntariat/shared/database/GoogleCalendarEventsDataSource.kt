package com.casavirupa.voluntariat.shared.database

import com.casavirupa.voluntariat.database.GoogleCalendarEventDb
import kotlinx.coroutines.flow.Flow

interface GoogleCalendarEventsDataSource {
    suspend fun insertGoogleCalendarEvent(events: List<GoogleCalendarEventDb>): Result<Unit>

    fun getGoogleCalendarEvents(): Flow<List<GoogleCalendarEventDb>>

    fun getGoogleCalendarEventsByDate(millis: Long): Flow<List<GoogleCalendarEventDb>>

    fun getGoogleCalendarEventsByRange(startMillis: Long, endMillis: Long): Flow<List<GoogleCalendarEventDb>>
}