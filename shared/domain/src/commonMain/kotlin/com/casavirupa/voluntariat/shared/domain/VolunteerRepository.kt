package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

interface VolunteerRepository {
    fun getVolunteersByDateRange(
        year: Int,
        monthNumber: Int,
        currentDate: LocalDate,
    ): Flow<List<Volunteer>>

    suspend fun getVolunteersByDate(date: LocalDate): Result<List<Volunteer>>

    fun getVolunteersByUserAndMonth(
        id: UserId,
        monthNumber: Int,
        year: Int,
    ): Flow<List<Volunteer>>

    suspend fun reserveDay(id: UserId, volunteer: Volunteer): Result<Unit>

    suspend fun deleteVolunteer(id: VolunteerId): Result<Unit>
}