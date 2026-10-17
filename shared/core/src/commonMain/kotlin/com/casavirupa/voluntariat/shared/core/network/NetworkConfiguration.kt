package com.casavirupa.voluntariat.shared.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
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
        }
        install(HttpTimeout) {
            // The Google Apps Script endpoint redirects and cold-starts; a hung request must
            // still give up so callers fall back to the cached events.
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    }
}

private const val REQUEST_TIMEOUT_MILLIS = 20_000L
