package com.casavirupa.voluntariat.shared.core.utils

import java.util.Locale

actual fun Double.formatString(decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", this)