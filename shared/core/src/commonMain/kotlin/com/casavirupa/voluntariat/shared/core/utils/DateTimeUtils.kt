package com.casavirupa.voluntariat.shared.core.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

fun getCurrentYear(timeZone: TimeZone = TimeZone.currentSystemDefault()) =
    Clock.System.now()
        .toLocalDateTime(timeZone)
        .year

fun getCurrentMonth(timeZone: TimeZone = TimeZone.currentSystemDefault()) =
    Clock.System.now()
        .toLocalDateTime(timeZone)
        .month

fun Long.toDate(timeZone: TimeZone = TimeZone.UTC) =
    Instant
        .fromEpochMilliseconds(this)
        .toDate(timeZone)

fun Instant.toDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    toLocalDateTime(timeZone).date

fun LocalDate.toMilliseconds() =
    this
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

expect fun LocalDate.format(pattern: String): String

@OptIn(FormatStringsInDatetimeFormats::class)
fun LocalTime.format(pattern: String) =
    LocalTime
        .Format { this.byUnicodePattern(pattern) }
        .format(this)