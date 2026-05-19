package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.responses.GoogleCalendarEventResponse
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class RemoteCalendarRepository(private val httpClient: HttpClient) : CalendarRepository {
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
}

private fun GoogleCalendarEventResponse.toDomainModel() =
    GoogleCalendarEvent(
        id = id,
        title = title,
        description = description,
        start = Instant.parse(start).toLocalDateTime(TimeZone.currentSystemDefault()),
        end = Instant.parse(start).toLocalDateTime(TimeZone.currentSystemDefault()),
    )
