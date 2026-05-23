package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.responses.GoogleCalendarEventResponse
import com.casavirupa.voluntariat.database.GoogleCalendarEventDb
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.END_MONTH_PARAM
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.END_YEAR_PARAM
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.SCRIPT_URL
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.START_MONTH_PARAM
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.START_YEAR_PARAM
import com.casavirupa.voluntariat.shared.core.utils.isoToEpochMilliseconds
import com.casavirupa.voluntariat.shared.core.utils.toEpochMilliseconds
import com.casavirupa.voluntariat.shared.core.utils.toLocalDateTime
import com.casavirupa.voluntariat.shared.database.GoogleCalendarEventsDataSource
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

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
        httpClient.get(SCRIPT_URL) {
            url {
                parameters.append(START_YEAR_PARAM, year.toString())
                parameters.append(START_MONTH_PARAM, startMonth.toString())
                parameters.append(END_YEAR_PARAM, endYear.toString())
                parameters.append(END_MONTH_PARAM, endMonth.toString())
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

    override fun getGoogleCalendarEventsByDate(date: LocalDate): Flow<List<GoogleCalendarEvent>> =
        googleCalendarEventsDataSource
            .getGoogleCalendarEventsByDate(date.toEpochMilliseconds())
            .map { it.map(GoogleCalendarEventDb::toDomainModel) }
}

private fun GoogleCalendarEventResponse.toDatabaseModel() =
    GoogleCalendarEventDb(
        id = 0L,
        googleId = id,
        title = title,
        description = description,
        start = start.isoToEpochMilliseconds(),
        end = end.isoToEpochMilliseconds(),
        isAllDay = isAllDay,
    )

private fun GoogleCalendarEventDb.toDomainModel() =
    GoogleCalendarEvent(
        id = id,
        googleId = googleId,
        title = title,
        description = description,
        start = start.toLocalDateTime(),
        end = end.toLocalDateTime(),
        isAllDay = isAllDay,
        available = title != NOT_AVAILABLE_TITLE
    )

private const val NOT_AVAILABLE_TITLE = "NO VOLUNTARIAT"
