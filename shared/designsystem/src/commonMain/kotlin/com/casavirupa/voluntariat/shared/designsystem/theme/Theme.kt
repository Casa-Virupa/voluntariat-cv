package com.casavirupa.voluntariat.shared.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val voluntariatCVColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = LightOrange,
    onPrimaryContainer = DarkGrayBrown,
    surface = Color.White,
    onSurface = GrayBrown,
    onSurfaceVariant = LightGrayBrown,
)

@Composable
fun VoluntariatCVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = voluntariatCVColorScheme,
        typography = VoluntariatCVTypography,
        content = content,
    )
}