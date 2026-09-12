package com.casavirupa.voluntariat.shared.database.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.casavirupa.voluntariat.database.CVDatabase
import org.koin.core.Koin

actual fun createPlatformDatabaseDriver(koin: Koin): SqlDriver =
    AndroidSqliteDriver(
        schema = CVDatabase.Schema,
        context = koin.get(),
        name = DATABASE_NAME
    )