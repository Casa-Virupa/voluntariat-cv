package com.casavirupa.voluntariat.shared.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.casavirupa.voluntariat.database.CVDatabase
import com.casavirupa.voluntariat.database.GoogleCalendarEventDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

internal class LocalGoogleCalendarEventsDataSource(
    database: CVDatabase,
): GoogleCalendarEventsDataSource {
    private val dbQueries = database.cVDatabaseQueries

    override suspend fun insertGoogleCalendarEvent(
        events: List<GoogleCalendarEventDb>,
    ): Result<Unit> = runCatching {
        dbQueries.transaction {
            dbQueries.deleteAllGoogleCalendarEvents()
            events.forEach { event ->
                with(event) {
                    dbQueries.insertGoogleCalendarEvent(
                        id = null,
                        googleId = googleId,
                        title = title,
                        description = description,
                        start = start,
                        end = end,
                        isAllDay = isAllDay,
                    )
                }
            }
        }
    }

    override fun getGoogleCalendarEvents(): Flow<List<GoogleCalendarEventDb>> =
        dbQueries
            .getGoogleCalendarEvents()
            .asFlow()
            .mapToList(Dispatchers.IO)

    override fun getGoogleCalendarEventsByDate(millis: Long): Flow<List<GoogleCalendarEventDb>> =
        dbQueries
            .getGoogleCalendarEventsByDate(millis)
            .asFlow()
            .mapToList(Dispatchers.IO)
}