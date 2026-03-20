package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.Event
import dev.gitlive.firebase.firestore.FirebaseFirestore

class FirebaseCalendarRepository(
    val firestore: FirebaseFirestore,
) : CalendarRepository {
    override suspend fun getCalendarEvents(monthNumber: Int): Result<List<Event>> =
        Result.success(emptyList())
}