package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Month

interface VolunteerRepository {
    fun getVolunteersByUserAndMonth(
        id: UserId,
        monthNumber: Int,
        year: Int,
    ): Flow<List<Volunteer>>

    suspend fun deleteVolunteer(id: VolunteerId): Result<Unit>
}