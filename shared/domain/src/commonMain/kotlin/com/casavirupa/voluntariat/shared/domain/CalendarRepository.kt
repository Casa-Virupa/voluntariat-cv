package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface CalendarRepository {
    /**
     * Makes sure the Google Calendar events of the given month and its two neighbours are in
     * the local database, fetching them unless they were synced recently. Reads never touch
     * the network: they observe the local database, which this call updates.
     */
    suspend fun syncGoogleCalendarEventsAround(year: Int, month: Int): Result<Unit>

    fun getGoogleCalendarEvents(): Flow<List<GoogleCalendarEvent>>

    fun getGoogleCalendarEventsByDate(date: LocalDate): Flow<List<GoogleCalendarEvent>>
}
