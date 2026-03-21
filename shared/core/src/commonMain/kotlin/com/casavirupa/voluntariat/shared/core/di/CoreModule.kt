package com.casavirupa.voluntariat.shared.core.di

import com.casavirupa.voluntariat.shared.core.network.NetworkConfiguration
import io.ktor.client.HttpClient
import org.koin.dsl.module

val coreModule = module {
    single<HttpClient> { NetworkConfiguration.httpClient() }
}