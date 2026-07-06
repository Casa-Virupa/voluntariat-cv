package com.casavirupa.voluntariat.shared.core.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.minus
import kotlinx.datetime.number
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

fun LocalDate.toEpochMilliseconds() =
    this
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

fun String.isoToEpochMilliseconds() = Instant.parse(this).toEpochMilliseconds()

fun Long.toLocalDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()) =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)

fun LocalDate.getFirstDayOfMonth() = LocalDate(year, month, 1)

fun LocalDate.getLastDayOfMonth(): LocalDate {
    val firstDayOfNextMonth = if (month.number == 12) {
        LocalDate(year + 1, 1, 1)
    } else {
        LocalDate(year, month.number + 1, 1)
    }
    return firstDayOfNextMonth.minus(DatePeriod(days = 1))
}

expect fun LocalDate.format(pattern: String): String

@OptIn(FormatStringsInDatetimeFormats::class)
fun LocalTime.format(pattern: String) =
    LocalTime
        .Format { this.byUnicodePattern(pattern) }
        .format(this)