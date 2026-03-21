package com.casavirupa.voluntariart.shared.data.repositories

import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.Event
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

class RemoteCalendarRepository(
    private val firestore: FirebaseFirestore,
    private val httpClient: HttpClient,
) : CalendarRepository {
    override suspend fun getCalendarEvents(year: Int, monthNumber: Int): Result<List<Event>> =
        Result.success(emptyList())

    override suspend fun getGoogleCalendarEvents(year: Int, monthNumber: Int): Result<Int> =
        runCatching {
            val response = httpClient.get {
                url {
                    parameters.append("year", year.toString())
                    parameters.append("month", monthNumber.toString())
                }
            }.body<List<GoogleCalendarEventResponse>>()
            Logger.d("asdd") { response.map { it.id }.toString() }
            response.size
        }
}

@Serializable
data class GoogleCalendarEventResponse(
    val id: String,
    val title: String,
    val description: String,
    val start: String,
    val end: String,
)