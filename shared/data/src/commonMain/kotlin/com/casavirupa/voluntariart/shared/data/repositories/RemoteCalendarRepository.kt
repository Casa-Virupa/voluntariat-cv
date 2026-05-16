package com.casavirupa.voluntariart.shared.data.repositories

import co.touchlab.kermit.Logger
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseVolunteer
import com.casavirupa.voluntariart.shared.data.repositories.responses.GoogleCalendarEventResponse
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants
import com.casavirupa.voluntariat.shared.core.utils.toMilliseconds
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class RemoteCalendarRepository(
    private val firestore: FirebaseFirestore,
    private val httpClient: HttpClient,
) : CalendarRepository {
    override fun getVolunteersByDateRange(
        year: Int,
        monthNumber: Int,
        currentDate: LocalDate,
    ): Flow<List<Volunteer>> {
        val timestamp = Timestamp.fromMilliseconds(currentDate.toMilliseconds().toDouble())
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

    override suspend fun getGoogleCalendarEvents(
        year: Int,
        monthNumber: Int,
    ): Result<List<GoogleCalendarEvent>> = runCatching {
        httpClient.get(CalendarConstants.SCRIPT_URL) {
            url {
                parameters.append("year", year.toString())
                parameters.append("month", monthNumber.toString())
            }
        }.body<List<GoogleCalendarEventResponse>>()
            .map(GoogleCalendarEventResponse::toDomainModel)
    }

    override suspend fun getVolunteersByDate(date: LocalDate): Result<List<Volunteer>> =
        runCatching {
            val timestamp = Timestamp.fromMilliseconds(date.toMilliseconds().toDouble())
            firestore
                .collection("volunteers")
                .where { "timestamp" equalTo timestamp }
                .get()
                .documents
                .map { document ->
                    document.data<FirebaseVolunteer>().toDomainModel(document.id)
                }
        }.onFailure {
            Logger.d("asdd", it)
        }
}

private fun GoogleCalendarEventResponse.toDomainModel() =
    GoogleCalendarEvent(
        id = id,
        title = title,
        description = description,
        start = Instant.parse(start).toLocalDateTime(TimeZone.currentSystemDefault()),
        end = Instant.parse(start).toLocalDateTime(TimeZone.currentSystemDefault()),
    )

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
            type = modelTypes.first(),
            timeRange = TimeRange.DefaultMorning,
        )
        "afternoon" -> Shift.Afternoon(
            type = modelTypes.first(),
            timeRange = TimeRange.DefaultMorning,
        )
        "all_day" -> Shift.AllDay(
            morningType = modelTypes.first(),
            morningTimeRange = TimeRange.DefaultMorning,
            afternoonType = modelTypes.last(),
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

private fun Timestamp.toDate() =
    Instant
        .fromEpochSeconds(seconds, nanoseconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
