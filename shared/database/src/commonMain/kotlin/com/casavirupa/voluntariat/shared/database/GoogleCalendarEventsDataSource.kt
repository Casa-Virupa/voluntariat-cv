package com.casavirupa.voluntariat.shared.database

import com.casavirupa.voluntariat.database.GoogleCalendarEventDb

interface GoogleCalendarEventsDataSource {
    suspend fun insertGoogleCalendarEvent(events: List<GoogleCalendarEventDb>): Result<Unit>
}