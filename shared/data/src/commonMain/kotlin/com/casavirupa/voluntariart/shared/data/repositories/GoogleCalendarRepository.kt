package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.responses.GoogleCalendarEventResponse
import com.casavirupa.voluntariat.database.GoogleCalendarEventDb
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.END_MONTH_PARAM
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.END_YEAR_PARAM
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.SCRIPT_URL
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.START_MONTH_PARAM
import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants.START_YEAR_PARAM
import com.casavirupa.voluntariat.shared.core.utils.isoToEpochMilliseconds
import com.casavirupa.voluntariat.shared.core.utils.toDate
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class GoogleCalendarRepository(
    private val httpClient: HttpClient,
    private val googleCalendarEventsDataSource: GoogleCalendarEventsDataSource,
) : CalendarRepository {
    // Per-month freshness, process-scoped (this repository is a Koin single). The events
    // themselves persist in the local database, so after a relaunch they show instantly while
    // the visible window is fetched again.
    private val syncedAt = mutableMapOf<YearMonth, Instant>()
    private val syncMutex = Mutex()

    override suspend fun syncGoogleCalendarEventsAround(year: Int, month: Int): Result<Unit> =
        syncMutex.withLock {
            val now = Clock.System.now()
            val centre = YearMonth(year, month)
            val stale = (centre.minusMonth()..centre.plusMonth()).filter { yearMonth ->
                syncedAt[yearMonth]?.let { now - it > SYNC_TTL } ?: true
            }
            if (stale.isEmpty()) return@withLock Result.success(Unit)

            // One request covering the stale months plus a few ahead: a call costs the same
            // whatever its span, and browsing forward month by month then rarely waits on it.
            val first = stale.first()
            val last = stale.last().plus(FETCH_AHEAD_MONTHS, DateTimeUnit.MONTH)
            fetchEvents(from = first, to = last)
                .mapCatching { events ->
                    googleCalendarEventsDataSource
                        .replaceGoogleCalendarEvents(
                            startMillis = first.firstDay.toEpochMilliseconds(),
                            endMillis = last.lastDay.plus(1, DateTimeUnit.DAY).toEpochMilliseconds(),
                            events = events,
                            // Past days never draw events, so anything that ended before last
                            // month is dead weight.
                            pruneEndingBeforeMillis = now.toDate().yearMonth.minusMonth().firstDay
                                .toEpochMilliseconds(),
                        )
                        .getOrThrow()
                }
                .onSuccess { (first..last).forEach { syncedAt[it] = now } }
        }

    private suspend fun fetchEvents(
        from: YearMonth,
        to: YearMonth,
    ): Result<List<GoogleCalendarEventDb>> = runCatching {
        httpClient.get(SCRIPT_URL) {
            url {
                parameters.append(START_YEAR_PARAM, from.year.toString())
                parameters.append(START_MONTH_PARAM, from.month.number.toString())
                parameters.append(END_YEAR_PARAM, to.year.toString())
                parameters.append(END_MONTH_PARAM, to.month.number.toString())
            }
        }.body<List<GoogleCalendarEventResponse>>()
            .map(GoogleCalendarEventResponse::toDatabaseModel)
    }

    override fun getGoogleCalendarEvents(): Flow<List<GoogleCalendarEvent>> =
        googleCalendarEventsDataSource
            .getGoogleCalendarEvents()
            .map { it.map(GoogleCalendarEventDb::toDomainModel) }

    override fun getGoogleCalendarEventsByDate(date: LocalDate): Flow<List<GoogleCalendarEvent>> =
        googleCalendarEventsDataSource
            .getGoogleCalendarEventsByDate(date.toEpochMilliseconds())
            .map { it.map(GoogleCalendarEventDb::toDomainModel) }

    companion object {
        /** How long a month's events are considered fresh before the calendar asks again. */
        val SYNC_TTL = 5.minutes

        /** Extra months fetched beyond the last stale one. */
        private const val FETCH_AHEAD_MONTHS = 2
    }
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
