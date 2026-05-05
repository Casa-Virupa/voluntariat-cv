package com.casavirupa.voluntariart.shared.data.repositories

import co.touchlab.kermit.Logger
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseVolunteer
import com.casavirupa.voluntariart.shared.data.repositories.responses.GoogleCalendarEventResponse
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.SpecificArea
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class RemoteCalendarRepository(
    private val firestore: FirebaseFirestore,
    private val httpClient: HttpClient,
) : CalendarRepository {
    override suspend fun getCalendarEvents(year: Int, monthNumber: Int): Result<List<Volunteer>> =
        runCatching {
            val monthStr = monthNumber.formatMonth()
            firestore
                .collection("reservations")
                .where {
                    "date" greaterThanOrEqualTo "$year-$monthStr-01"
                    "date" lessThanOrEqualTo "$year-$monthStr-31"
                }
                .get()
                .documents
                .map { document ->
                    document.data<FirebaseVolunteer>().toDomainModel(document.id)
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

    override suspend fun reserveDay(
        id: UserId,
        volunteer: Volunteer,
    ): Result<Unit> = runCatching {
        firestore
            .collection("reservations")
            .add(volunteer.toFirebaseModel(id))
    }

    override suspend fun getVolunteersByDate(date: LocalDate): Result<List<Volunteer>> =
        runCatching {
            firestore
                .collection("reservations")
                .where { "date" equalTo date.toString() }
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

private fun Volunteer.toFirebaseModel(userId: UserId) =
    FirebaseVolunteer(
        userId = userId.value,
        date = date.toString(),
        shift = volunteerShift.toFirebaseValue(),
        specificArea = specificArea?.toFirebaseValue(),
        mealTypes = meals.map(Meal::toFirebaseValue),
        sleep = sleep,
    )

private fun FirebaseVolunteer.toDomainModel(docId: String) =
    Volunteer(
        id = VolunteerId(docId),
        userId = UserId(userId),
        date = LocalDate.parse(date),
        volunteerShift = shift.toVolunteerShiftModel(),
        specificArea = specificArea.toSpecificAreaModel(),
        meals = mealTypes.map(String::toMealTypeModel),
        sleep = sleep,
    )

private fun Shift.toFirebaseValue() =
    when (this) {
        Shift.Morning -> "morning"
        Shift.Afternoon -> "afternoon"
        Shift.AllDay -> "all_day"
        else -> EMPTY_VALUE
    }

private fun SpecificArea?.toFirebaseValue() =
    when (this) {
        SpecificArea.Morning -> "morning"
        SpecificArea.Afternoon -> "afternoon"
        SpecificArea.AllDay -> "all_day"
        else -> EMPTY_VALUE
    }

private fun Meal.toFirebaseValue() =
    when (this) {
        Meal.Lunch -> "lunch"
        Meal.Dinner -> "dinner"
        else -> EMPTY_VALUE
    }

private fun String.toVolunteerShiftModel() =
    when (this) {
        "morning" -> Shift.Morning
        "afternoon" -> Shift.Afternoon
        "all_day" -> Shift.AllDay
        else -> Shift.Unknown
    }

private fun String?.toSpecificAreaModel() =
    when (this) {
        "morning" -> SpecificArea.Morning
        "afternoon" -> SpecificArea.Afternoon
        "all_day" -> SpecificArea.AllDay
        else -> null
    }

private fun String.toMealTypeModel() =
    when (this) {
        "lunch" -> Meal.Lunch
        "dinner" -> Meal.Dinner
        else -> Meal.Unknown
    }

private const val EMPTY_VALUE = ""

private fun Int.formatMonth(): String = this.toString().padStart(2, '0')