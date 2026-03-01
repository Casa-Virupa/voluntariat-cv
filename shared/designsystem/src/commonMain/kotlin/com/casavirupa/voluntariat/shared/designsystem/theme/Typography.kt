package com.casavirupa.voluntariat.shared.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import voluntariatcv.shared.designsystem.generated.resources.Res
import voluntariatcv.shared.designsystem.generated.resources.dmsans_bold
import voluntariatcv.shared.designsystem.generated.resources.dmsans_medium
import voluntariatcv.shared.designsystem.generated.resources.dmsans_regular
import voluntariatcv.shared.designsystem.generated.resources.kalice_bold
import voluntariatcv.shared.designsystem.generated.resources.kalice_bolditalic
import voluntariatcv.shared.designsystem.generated.resources.kalice_extrabolditalic
import voluntariatcv.shared.designsystem.generated.resources.kalice_italic
import voluntariatcv.shared.designsystem.generated.resources.kalice_medium
import voluntariatcv.shared.designsystem.generated.resources.kalice_mediumitalic
import voluntariatcv.shared.designsystem.generated.resources.kalice_regular

private val kalice: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.kalice_extrabolditalic, FontWeight.ExtraBold, FontStyle.Italic),
        Font(Res.font.kalice_bold, FontWeight.Bold),
        Font(Res.font.kalice_bolditalic, FontWeight.Bold, FontStyle.Italic),
        Font(Res.font.kalice_medium, FontWeight.Medium),
        Font(Res.font.kalice_mediumitalic, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.kalice_regular, FontWeight.Normal),
        Font(Res.font.kalice_italic, FontWeight.Normal, FontStyle.Italic),
    )

private val DMSans: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.dmsans_bold, FontWeight.Bold),
        Font(Res.font.dmsans_medium, FontWeight.Medium),
        Font(Res.font.dmsans_regular, FontWeight.Normal)
    )

val VoluntariatCVTypography: Typography
    @Composable
    get() = Typography(
        displayMedium = TextStyle(
            fontFamily = kalice,
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            lineHeight = 45.5.sp,
            letterSpacing = (-0.75).sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = DMSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = DMSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 1.4.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = DMSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.6.sp,
        )
    )