package com.casavirupa.voluntariat.shared.core.utils

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual fun Double.formatString(decimals: Int): String {
    val formatter = NSNumberFormatter().apply {
        minimumFractionDigits = 0uL
        maximumFractionDigits = decimals.toULong()
        numberStyle = NSNumberFormatterDecimalStyle
    }
    return formatter.stringFromNumber(NSNumber(this)) ?: ""
}