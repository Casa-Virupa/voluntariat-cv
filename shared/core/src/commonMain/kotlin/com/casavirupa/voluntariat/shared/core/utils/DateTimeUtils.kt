package com.casavirupa.voluntariat.shared.core.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

fun getCurrentYear(timeZone: TimeZone = TimeZone.currentSystemDefault()) =
    Clock.System.now()
        .toLocalDateTime(timeZone)
        .year

fun getCurrentMonth(timeZone: TimeZone = TimeZone.currentSystemDefault()) =
    Clock.System.now()
        .toLocalDateTime(timeZone)
        .month