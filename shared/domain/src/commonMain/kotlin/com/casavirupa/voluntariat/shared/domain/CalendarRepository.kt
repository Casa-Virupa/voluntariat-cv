package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.Event

interface CalendarRepository {
    suspend fun getCalendarEvents(year: Int, monthNumber: Int): Result<List<Event>>

    suspend fun getGoogleCalendarEvents(year: Int, monthNumber: Int): Result<Int>
}