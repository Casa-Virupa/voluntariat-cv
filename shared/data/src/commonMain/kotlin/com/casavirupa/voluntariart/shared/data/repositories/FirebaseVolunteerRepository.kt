package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseTimeRange
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseVolunteer
import com.casavirupa.voluntariat.shared.core.utils.toEpochMilliseconds
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
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
    override fun getVolunteersByDateRange(
        year: Int,
        monthNumber: Int,
        currentDate: LocalDate,
    ): Flow<List<Volunteer>> {
        val timestamp = Timestamp.fromMilliseconds(currentDate.toEpochMilliseconds().toDouble())
        return firestore
            .collection("volunteers")
            .where { "timestamp" greaterThanOrEqualTo timestamp }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.data<FirebaseVolunteer>().toDomainModel(doc.id)
                }
            }
    }

    override suspend fun getVolunteersByDate(date: LocalDate): Result<List<Volunteer>> =
        runCatching {
            val timestamp = Timestamp.fromMilliseconds(date.toEpochMilliseconds().toDouble())
            firestore
                .collection("volunteers")
                .where { "timestamp" equalTo timestamp }
                .get()
                .documents
                .map { document ->
                    document.data<FirebaseVolunteer>().toDomainModel(document.id)
                }
        }

    override suspend fun getVolunteersByUser(id: UserId): Result<List<Volunteer>> =
        runCatching {
            firestore
                .collection("volunteers")
                .where { "userId" equalTo id.value }
                .get()
                .documents
                .map { document ->
                    document.data<FirebaseVolunteer>().toDomainModel(document.id)
                }
        }

    override fun getVolunteersByUserAndMonth(
        id: UserId,
        monthNumber: Int,
        year: Int,
    ): Flow<List<Volunteer>> {
        val startTimestamp = Timestamp.fromMilliseconds(
            LocalDate(year, monthNumber, 1).toEpochMilliseconds().toDouble()
        )
        val nextMonth = if (monthNumber == 12) 1 else monthNumber + 1
        val nextYear = if (monthNumber == 12) year + 1 else year
        val endTimestamp = Timestamp.fromMilliseconds(
            LocalDate(nextYear, nextMonth, 1).toEpochMilliseconds().toDouble()
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
                    doc
                        .data<FirebaseVolunteer>()
                        .toDomainModel(doc.id)
                }
            }
    }

    override suspend fun reserveDay(
        id: UserId,
        volunteer: Volunteer,
    ): Result<Unit> = runCatching {
        firestore
            .collection("volunteers")
            .add(volunteer.toFirebaseModel(id))
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
        shift = shift.toVolunteerShiftModel(types),
        meals = mealTypes.map(String::toMealTypeModel),
        sleep = sleep,
    )

private fun String.toVolunteerShiftModel(types: List<String>): Shift {
    val modelTypes = types.map(String::toVolunteerTypeModel)
    return when (this) {
        "morning" -> Shift.Morning(
            type = modelTypes.firstOrNull() ?: VolunteerType.Unknown,
            timeRange = TimeRange.DefaultMorning,
        )
        "afternoon" -> Shift.Afternoon(
            type = modelTypes.firstOrNull() ?: VolunteerType.Unknown,
            timeRange = TimeRange.DefaultMorning,
        )
        "all_day" -> Shift.AllDay(
            morningType = modelTypes.firstOrNull() ?: VolunteerType.Unknown,
            morningTimeRange = TimeRange.DefaultMorning,
            afternoonType = modelTypes.lastOrNull() ?: VolunteerType.Unknown,
            afternoonTimeRange = TimeRange.DefaultMorning,
        )
        else -> Shift.Unknown
    }
}

private fun String.toVolunteerTypeModel() =
    when (this) {
        "general" -> VolunteerType.General
        "specific" -> VolunteerType.Specific
        else -> VolunteerType.Unknown
    }

private fun String.toMealTypeModel() =
    when (this) {
        "lunch" -> Meal.Lunch
        "dinner" -> Meal.Dinner
        else -> Meal.Unknown
    }

private fun Volunteer.toFirebaseModel(userId: UserId) =
    FirebaseVolunteer(
        userId = userId.value,
        timestamp = Timestamp.fromMilliseconds(date.toEpochMilliseconds().toDouble()),
        shift = shift.toFirebaseValue(),
        types = shift.getTypes(),
        timeRanges = shift.getTimeRanges(),
        mealTypes = meals.map(Meal::toFirebaseValue),
        sleep = sleep,
    )

private fun Shift.toFirebaseValue() =
    when (this) {
        is Shift.Morning -> "morning"
        is Shift.Afternoon -> "afternoon"
        is Shift.AllDay -> "all_day"
        else -> EMPTY_VALUE
    }

private fun Shift.getTypes() =
    when (this) {
        is Shift.Morning -> listOf(type.toFirebaseValue())
        is Shift.Afternoon -> listOf(type.toFirebaseValue())
        is Shift.AllDay -> listOf(morningType.toFirebaseValue(), afternoonType.toFirebaseValue())
        else -> emptyList()
    }

private fun VolunteerType.toFirebaseValue() =
    when (this) {
        VolunteerType.General -> "general"
        VolunteerType.Specific -> "specific"
        else -> EMPTY_VALUE
    }

private fun Shift.getTimeRanges() =
    when (this) {
        is Shift.Morning -> listOf(timeRange.toFirebaseTimeRange())
        is Shift.Afternoon -> listOf(timeRange.toFirebaseTimeRange())
        is Shift.AllDay -> listOf(
            morningTimeRange.toFirebaseTimeRange(),
            afternoonTimeRange.toFirebaseTimeRange()
        )
        else -> listOf()
    }

private fun TimeRange.toFirebaseTimeRange() =
    FirebaseTimeRange(
        start = start,
        end = end,
    )

private fun Meal.toFirebaseValue() =
    when (this) {
        Meal.Lunch -> "lunch"
        Meal.Dinner -> "dinner"
        else -> EMPTY_VALUE
    }

private fun Timestamp.toDate() =
    Instant
        .fromEpochSeconds(seconds, nanoseconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

private const val EMPTY_VALUE = ""
