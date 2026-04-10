package com.casavirupa.voluntariart.shared.data.repositories

import co.touchlab.kermit.Logger
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseReservation
import com.casavirupa.voluntariart.shared.data.repositories.responses.GoogleCalendarEventResponse
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.Event
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.Reservation
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.parameters
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class RemoteCalendarRepository(
    private val firestore: FirebaseFirestore,
    private val httpClient: HttpClient,
) : CalendarRepository {
    override suspend fun getCalendarEvents(year: Int, monthNumber: Int): Result<List<Event>> =
        Result.success(emptyList())

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
        reservation: Reservation,
    ): Result<Unit> = runCatching {
        firestore
            .collection("reservations")
            .add(reservation.toFirebaseModel(id))
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

private fun Reservation.toFirebaseModel(userId: UserId) =
    FirebaseReservation(
        userId = userId.value,
        date = date.toString(),
        scheduleRange = scheduleRange.name,
        technicalAreaTurn = technicalAreaTurn.name,
        mealType = mealType.name,
        sleep = sleep,
    )