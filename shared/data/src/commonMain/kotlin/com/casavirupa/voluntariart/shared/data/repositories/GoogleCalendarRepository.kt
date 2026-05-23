package com.casavirupa.voluntariart.shared.data.repositories

import co.touchlab.kermit.Logger
import com.casavirupa.voluntariart.shared.data.repositories.responses.GoogleCalendarEventResponse
import com.casavirupa.voluntariat.database.GoogleCalendarEventDb
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants
import com.casavirupa.voluntariat.shared.database.GoogleCalendarEventsDataSource
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.collections.map
import kotlin.time.Instant

class GoogleCalendarRepository(
    private val httpClient: HttpClient,
    private val googleCalendarEventsDataSource: GoogleCalendarEventsDataSource,
) : CalendarRepository {
    override suspend fun syncGoogleCalendarEvents(
        year: Int,
        startMonth: Int,
        endMonth: Int,
    ): Result<Unit> = runCatching {
        val endYear = if (endMonth < startMonth) year + 1 else year
        httpClient.get(CalendarConstants.SCRIPT_URL) {
            url {
                parameters.append("startYear", year.toString())
                parameters.append("startMonth", startMonth.toString())
                parameters.append("endYear", endYear.toString())
                parameters.append("endMonth", endMonth.toString())
            }
        }.body<List<GoogleCalendarEventResponse>>()
            .map(GoogleCalendarEventResponse::toDatabaseModel)
    }.mapCatching { events ->
        googleCalendarEventsDataSource.insertGoogleCalendarEvent(events)
    }

    override fun getGoogleCalendarEvents(): Flow<List<GoogleCalendarEvent>> =
        googleCalendarEventsDataSource
            .getGoogleCalendarEvents()
            .map { it.map(GoogleCalendarEventDb::toDomainModel) }
}

private fun GoogleCalendarEventResponse.toDatabaseModel() =
    GoogleCalendarEventDb(
        id = 0L,
        googleId = id,
        title = title,
        description = description,
        start = Instant.parse(start).toEpochMilliseconds(),
        end = Instant.parse(end).toEpochMilliseconds(),
    )

private fun GoogleCalendarEventDb.toDomainModel() =
    GoogleCalendarEvent(
        id = googleId,
        title = title,
        description = description,
        start = Instant.fromEpochMilliseconds(start).toLocalDateTime(TimeZone.currentSystemDefault()),
        end = Instant.fromEpochMilliseconds(end).toLocalDateTime(TimeZone.currentSystemDefault()),
    )
