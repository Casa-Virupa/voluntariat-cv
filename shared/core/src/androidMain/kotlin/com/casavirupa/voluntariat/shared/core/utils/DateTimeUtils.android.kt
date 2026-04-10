package com.casavirupa.voluntariat.shared.core.utils

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
actual fun kotlinx.datetime.LocalDate.format(pattern: String): String {
    val javaDate = this.toJavaLocalDate()
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return javaDate.format(formatter)
}