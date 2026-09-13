package com.casavirupa.voluntariat.shared.database

import com.casavirupa.voluntariat.database.GoogleCalendarEventDb
import kotlinx.coroutines.flow.Flow

interface GoogleCalendarEventsDataSource {
    /**
     * Replaces the events overlapping [startMillis, endMillis) with [events] (a freshly fetched
     * window) and drops every event that ended before [pruneEndingBeforeMillis], so the table
     * stays bounded to the months the user can still see.
     */
    suspend fun replaceGoogleCalendarEvents(
        startMillis: Long,
        endMillis: Long,
        events: List<GoogleCalendarEventDb>,
        pruneEndingBeforeMillis: Long,
    ): Result<Unit>

    fun getGoogleCalendarEvents(): Flow<List<GoogleCalendarEventDb>>

    fun getGoogleCalendarEventsByDate(millis: Long): Flow<List<GoogleCalendarEventDb>>
}
