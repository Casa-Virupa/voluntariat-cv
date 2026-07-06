package com.casavirupa.voluntariat.shared.core.utils

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun Double.formatString(decimals: Int): String =
    NSString.stringWithFormat("%.${decimals}f", this)