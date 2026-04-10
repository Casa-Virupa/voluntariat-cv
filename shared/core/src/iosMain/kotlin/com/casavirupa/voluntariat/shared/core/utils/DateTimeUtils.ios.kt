package com.casavirupa.voluntariat.shared.core.utils

import kotlinx.datetime.number
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun kotlinx.datetime.LocalDate.format(pattern: String): String {
    val components = NSDateComponents().apply {
        year = this@format.year.toLong()
        month = this@format.month.number.toLong()
        day = this@format.day.toLong()
    }

    val calendar = NSCalendar.currentCalendar
    val date = calendar.dateFromComponents(components)
        ?: return this.toString()

    val formatter = NSDateFormatter().apply {
        dateFormat = pattern
        locale = NSLocale.currentLocale
    }

    return formatter.stringFromDate(date)
}