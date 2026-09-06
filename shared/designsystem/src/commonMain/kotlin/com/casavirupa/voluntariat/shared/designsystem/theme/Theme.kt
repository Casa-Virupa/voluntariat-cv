package com.casavirupa.voluntariat.shared.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val voluntariatCVColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = LightOrange100,
    onPrimaryContainer = DarkGrayBrown,
    // Used to highlight the viewer's own volunteering days in the calendar
    tertiary = Lilac,
    onTertiary = Color.White,
    surfaceContainerLowest = LightOrange50,
    surfaceContainerLow = LightOrange200,
    surface = Color.White,
    onSurface = GrayBrown,
    onSurfaceVariant = LightGrayBrown,
    inverseOnSurface = DarkGray,
    error = Red,
    outlineVariant = LightGray,
)

@Composable
fun VoluntariatCVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = voluntariatCVColorScheme,
        typography = VoluntariatCVTypography,
        content = content,
    )
}