package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.Event

interface CalendarRepository {
    suspend fun getCalendarEvents(monthNumber: Int): Result<List<Event>>
}