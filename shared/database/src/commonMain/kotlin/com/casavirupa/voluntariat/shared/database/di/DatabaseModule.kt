package com.casavirupa.voluntariat.shared.database.di

import app.cash.sqldelight.db.SqlDriver
import com.casavirupa.voluntariat.database.CVDatabase
import com.casavirupa.voluntariat.shared.database.GoogleCalendarEventsDataSource
import com.casavirupa.voluntariat.shared.database.LocalGoogleCalendarEventsDataSource
import org.koin.core.Koin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val databaseModule = module {
    single { createAppDatabase(getKoin()) }
    singleOf(::LocalGoogleCalendarEventsDataSource) bind GoogleCalendarEventsDataSource::class
}

private fun createAppDatabase(koin: Koin) =
    CVDatabase(driver = createPlatformDatabaseDriver(koin))

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect fun createPlatformDatabaseDriver(koin: Koin): SqlDriver

internal const val DATABASE_NAME = "cv_database.db"