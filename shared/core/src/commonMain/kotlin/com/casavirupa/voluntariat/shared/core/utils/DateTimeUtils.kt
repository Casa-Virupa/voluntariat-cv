package com.casavirupa.voluntariat.shared.core.utils

import com.casavirupa.voluntariat.shared.model.calendar.CasaVirupaTimeZone
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

fun getCurrentYear(timeZone: TimeZone = CasaVirupaTimeZone) =
    Clock.System.now()
        .toLocalDateTime(timeZone)
        .year

fun getCurrentMonth(timeZone: TimeZone = CasaVirupaTimeZone) =
    Clock.System.now()
        .toLocalDateTime(timeZone)
        .month

fun Long.toDate(timeZone: TimeZone = TimeZone.UTC) =
    Instant
        .fromEpochMilliseconds(this)
        .toDate(timeZone)

fun Instant.toDate(timeZone: TimeZone = CasaVirupaTimeZone): LocalDate =
    toLocalDateTime(timeZone).date

fun LocalDate.toEpochMilliseconds(timeZone: TimeZone = CasaVirupaTimeZone) =
    this
        .atStartOfDayIn(timeZone)
        .toEpochMilliseconds()

fun String.isoToEpochMilliseconds() = Instant.parse(this).toEpochMilliseconds()

fun Long.toLocalDateTime(timeZone: TimeZone = CasaVirupaTimeZone) =
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