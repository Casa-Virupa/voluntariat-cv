package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseVolunteer
import com.casavirupa.voluntariat.shared.core.utils.toMilliseconds
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.fromMilliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class FirebaseVolunteerRepository(
    private val firestore: FirebaseFirestore,
) : VolunteerRepository {
    override fun getVolunteersByUserAndMonth(
        id: UserId,
        monthNumber: Int,
        year: Int,
    ): Flow<List<Volunteer>> {
        val startTimestamp = Timestamp.fromMilliseconds(
            LocalDate(year, monthNumber, 1).toMilliseconds().toDouble()
        )
        val nextMonth = if (monthNumber == 12) 1 else monthNumber + 1
        val nextYear = if (monthNumber == 12) year + 1 else year
        val endTimestamp = Timestamp.fromMilliseconds(
            LocalDate(nextYear, nextMonth, 1).toMilliseconds().toDouble()
        )

        return firestore
            .collection("volunteers")
            .where {
                ("userId" equalTo id.value) and (
                    ("timestamp" greaterThanOrEqualTo startTimestamp) and
                    ("timestamp" lessThan endTimestamp)
                )
            }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.data<FirebaseVolunteer>().toDomainModel(doc.id)
                }
            }
    }

    override suspend fun deleteVolunteer(id: VolunteerId): Result<Unit> =
        runCatching {
            firestore
                .collection("volunteers")
                .document(id.value)
                .delete()
        }
}

private fun FirebaseVolunteer.toDomainModel(docId: String) =
    Volunteer(
        id = VolunteerId(docId),
        userId = UserId(userId),
        date = timestamp.toDate(),
        shift = Shift.Unknown,
        meals = mealTypes.map(String::toMealTypeModel),
        sleep = sleep,
    )

/*
private fun String.toVolunteerShiftModel() =
    when (this) {
        "morning" -> Shift.Morning
        "afternoon" -> Shift.Afternoon
        "all_day" -> Shift.AllDay
        else -> Shift.Unknown
    }
 */

private fun String.toMealTypeModel() =
    when (this) {
        "lunch" -> Meal.Lunch
        "dinner" -> Meal.Dinner
        else -> Meal.Unknown
    }

private fun Timestamp.toDate() =
    Instant
        .fromEpochSeconds(seconds, nanoseconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date