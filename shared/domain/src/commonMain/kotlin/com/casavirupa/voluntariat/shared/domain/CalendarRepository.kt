package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface CalendarRepository {
    fun getVolunteersByDateRange(
        year: Int,
        monthNumber: Int,
        currentDate: LocalDate,
    ): Flow<List<Volunteer>>

    suspend fun getGoogleCalendarEvents(
        year: Int,
        monthNumber: Int,
    ): Result<List<GoogleCalendarEvent>>

    suspend fun getVolunteersByDate(date: LocalDate): Result<List<Volunteer>>
}