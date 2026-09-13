package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseShift
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseTimeRange
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseVolunteer
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseVolunteerType
import com.casavirupa.voluntariat.shared.core.utils.toEpochMilliseconds
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.fromMilliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class FirebaseVolunteerRepository(
    private val firestore: FirebaseFirestore,
) : VolunteerRepository {
    override fun getVolunteersFrom(from: LocalDate): Flow<List<Volunteer>> {
        val timestamp = Timestamp.fromMilliseconds(from.toEpochMilliseconds().toDouble())
        return firestore
            .collection("volunteers")
            .where { "timestamp" greaterThanOrEqualTo timestamp }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.data<FirebaseVolunteer>().toDomainModel(doc.id)
                }
            }
            // Decoding the whole collection is the expensive part: keep it off the main thread.
            .flowOn(Dispatchers.Default)
            // A read error (e.g. rules) must not kill the calendar's state flows.
            .catch { emit(emptyList()) }
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
                .where { "user_id" equalTo id.value }
                .get()
                .documents
                .map { document ->
                    document.data<FirebaseVolunteer>().toDomainModel(document.id)
                }
        }

    override fun getVolunteersByUserFlow(id: UserId): Flow<List<Volunteer>> =
        firestore
            .collection("volunteers")
            .where { "user_id" equalTo id.value }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc
                        .data<FirebaseVolunteer>()
                        .toDomainModel(doc.id)
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
        return getVolunteersByUserAndRange(id, startTimestamp, endTimestamp)
    }

    override fun getVolunteersByUserAndQuarter(
        id: UserId,
        quarter: Int,
        year: Int,
    ): Flow<List<Volunteer>> {
        val startMonth = (quarter - 1) * 3 + 1
        val endMonth = quarter * 3
        val startTimestamp = Timestamp.fromMilliseconds(
            LocalDate(year, startMonth, 1).toEpochMilliseconds().toDouble()
        )
        val nextMonth = if (endMonth == 12) 1 else endMonth + 1
        val nextYear = if (endMonth == 12) year + 1 else year
        val endTimestamp = Timestamp.fromMilliseconds(
            LocalDate(nextYear, nextMonth, 1).toEpochMilliseconds().toDouble()
        )
        return getVolunteersByUserAndRange(id, startTimestamp, endTimestamp)
    }

    private fun getVolunteersByUserAndRange(
        id: UserId,
        startTimestamp: Timestamp,
        endTimestamp: Timestamp,
    ): Flow<List<Volunteer>> {
        return firestore
            .collection("volunteers")
            .where {
                ("user_id" equalTo id.value) and (
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
        shifts = shifts.mapNotNull { it.toDomainModelOrNull() },
        meals = mealTypes.map(String::toMealTypeModel),
        sleep = sleep,
    )

private fun FirebaseShift.toDomainModelOrNull() =
    when (shift) {
        "morning" -> Shift.Morning(
            type = type.toDomainModel(),
            timeRange = timeRange.toDomainModel(),
            online = online,
        )
        "afternoon" -> Shift.Afternoon(
            type = type.toDomainModel(),
            timeRange = timeRange.toDomainModel(),
            online = online,
        )
        else -> null
    }

private fun FirebaseVolunteerType.toDomainModel() =
    when (type) {
        "general" -> VolunteerType.General
        else -> VolunteerType.Specific(specificAreas!!.toSpecificArea())
    }

private fun String.toMealTypeModel() =
    when (this) {
        "breakfast" -> Meal.Breakfast
        "breakfast_next_day" -> Meal.BreakfastNextDay
        "lunch" -> Meal.Lunch
        "dinner" -> Meal.Dinner
        else -> Meal.Unknown
    }

private fun Volunteer.toFirebaseModel(userId: UserId) =
    FirebaseVolunteer(
        userId = userId.value,
        timestamp = Timestamp.fromMilliseconds(date.toEpochMilliseconds().toDouble()),
        shifts = shifts.map { it.toFirebaseModel() },
        mealTypes = meals.map(Meal::toFirebaseModel),
        sleep = sleep,
    )

private fun Shift.toFirebaseModel() =
    when (this) {
        is Shift.Morning -> FirebaseShift(
            shift = "morning",
            timeRange = timeRange.toFirebaseTimeRange(),
            type = type.toFirebaseModel(),
            online = online,
        )
        is Shift.Afternoon -> FirebaseShift(
            shift = "afternoon",
            timeRange = timeRange.toFirebaseTimeRange(),
            type = type.toFirebaseModel(),
            online = online,
        )
    }

private fun VolunteerType.toFirebaseModel() =
    FirebaseVolunteerType(
        type = when (this) {
            VolunteerType.General -> "general"
            is VolunteerType.Specific -> "specific"
        },
        specificAreas = when (this) {
            VolunteerType.General -> null
            is VolunteerType.Specific -> specificArea.toFirebaseValue()
        }
    )
private fun TimeRange.toFirebaseTimeRange() =
    FirebaseTimeRange(
        start = start,
        end = end,
    )

private fun Meal.toFirebaseModel() =
    when (this) {
        Meal.Breakfast -> "breakfast"
        Meal.BreakfastNextDay -> "breakfast_next_day"
        Meal.Lunch -> "lunch"
        Meal.Dinner -> "dinner"
        else -> EMPTY_VALUE
    }

private fun Timestamp.toDate() =
    Instant
        .fromEpochSeconds(seconds, nanoseconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

private fun FirebaseTimeRange.toDomainModel() =
    TimeRange(
        start = start,
        end = end,
    )

private fun SpecificArea.toFirebaseValue() =
    when (this) {
        SpecificArea.Animals -> "animals"
        SpecificArea.Shop -> "shop"
        SpecificArea.Communication -> "communication"
        SpecificArea.VolunteerCoordination -> "volunteer_coordination"
        SpecificArea.Kitchen -> "kitchen"
        SpecificArea.GraphicalDesign -> "graphical_design"
        SpecificArea.VirupaEditions -> "virupa_editions"
        SpecificArea.Exterior -> "exterior"
        SpecificArea.CanBordoiEvents -> "can_bordoi_events"
        SpecificArea.Grove -> "grove"
        SpecificArea.Registrations -> "registrations"
        SpecificArea.Gardening -> "gardening"
        SpecificArea.Labor -> "labor"
        SpecificArea.Maintenance -> "maintenance"
        SpecificArea.Pedagogical -> "pedagogical"
        SpecificArea.CommunityHealth -> "community_health"
        SpecificArea.Grants -> "grants"
        SpecificArea.Temple -> "temple"
        SpecificArea.Transcriptions -> "transcriptions"
        SpecificArea.TechnicalAndAudiovisual -> "technical_and_audiovisual"
        SpecificArea.TechnicalAndTexts -> "technical_and_texts"
        SpecificArea.Unknown -> ""
    }

private const val EMPTY_VALUE = ""
