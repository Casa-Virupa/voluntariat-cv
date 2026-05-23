package com.casavirupa.voluntariat.shared.database

import com.casavirupa.voluntariat.database.CVDatabase
import com.casavirupa.voluntariat.database.GoogleCalendarEventDb

internal class LocalGoogleCalendarEventsDataSource(
    database: CVDatabase,
): GoogleCalendarEventsDataSource {
    private val dbQueries = database.cVDatabaseQueries

    override suspend fun insertGoogleCalendarEvent(
        events: List<GoogleCalendarEventDb>,
    ): Result<Unit> = runCatching {
        dbQueries.transaction {
            events.forEach { event ->
                dbQueries.insertGoogleCalendarEvent(event)
            }
        }
    }
}