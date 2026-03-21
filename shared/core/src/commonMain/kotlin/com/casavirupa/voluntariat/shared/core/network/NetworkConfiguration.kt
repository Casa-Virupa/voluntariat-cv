package com.casavirupa.voluntariat.shared.core.network

import com.casavirupa.voluntariat.shared.core.constants.CalendarConstants
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object NetworkConfiguration {
    fun httpClient() = HttpClient {
        install(Logging) {
            logger = Logger.DEFAULT
        }
        install(ContentNegotiation) {
            json(Json { prettyPrint = true })
        }
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            url(CalendarConstants.SCRIPT_URL)
        }
    }
}