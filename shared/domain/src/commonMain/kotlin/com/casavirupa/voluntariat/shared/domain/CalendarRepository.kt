package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.Event
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate

interface CalendarRepository {
    suspend fun getCalendarEvents(year: Int, monthNumber: Int): Result<List<Volunteer>>

    suspend fun getGoogleCalendarEvents(
        year: Int,
        monthNumber: Int,
    ): Result<List<GoogleCalendarEvent>>

    suspend fun reserveDay(id: UserId, volunteer: Volunteer): Result<Unit>

    suspend fun getVolunteersByDate(date: LocalDate): Result<List<Volunteer>>
}