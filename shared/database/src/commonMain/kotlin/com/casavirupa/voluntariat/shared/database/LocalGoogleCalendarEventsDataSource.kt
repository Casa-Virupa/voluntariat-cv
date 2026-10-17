package com.casavirupa.voluntariat.shared.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.casavirupa.voluntariat.database.CVDatabase
import com.casavirupa.voluntariat.database.GoogleCalendarEventDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class LocalGoogleCalendarEventsDataSource(
    database: CVDatabase,
): GoogleCalendarEventsDataSource {
    private val dbQueries = database.cVDatabaseQueries

    override suspend fun replaceGoogleCalendarEvents(
        startMillis: Long,
        endMillis: Long,
        events: List<GoogleCalendarEventDb>,
        pruneEndingBeforeMillis: Long,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            dbQueries.transaction {
                dbQueries.deleteGoogleCalendarEventsOverlapping(
                    endTimestamp = endMillis,
                    startTimestamp = startMillis,
                )
                dbQueries.deleteGoogleCalendarEventsEndingBefore(pruneEndingBeforeMillis)
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
