package com.casavirupa.voluntariat.shared.database.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.casavirupa.voluntariat.database.CVDatabase
import org.koin.core.Koin

@Suppress(names = ["NO_ACTUAL_FOR_EXPECT"])
actual fun createPlatformDatabaseDriver(koin: Koin): SqlDriver =
    NativeSqliteDriver(
        schema = CVDatabase.Schema,
        name = DATABASE_NAME,
    )