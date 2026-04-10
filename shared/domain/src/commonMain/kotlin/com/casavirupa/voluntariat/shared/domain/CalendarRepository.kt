package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.Event
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.Reservation
import com.casavirupa.voluntariat.shared.model.user.UserId

interface CalendarRepository {
    suspend fun getCalendarEvents(year: Int, monthNumber: Int): Result<List<Event>>

    suspend fun getGoogleCalendarEvents(
        year: Int,
        monthNumber: Int,
    ): Result<List<GoogleCalendarEvent>>

    suspend fun reserveDay(id: UserId,  reservation: Reservation): Result<Unit>
}