package com.casavirupa.voluntariat.shared.common.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import voluntariatcv.shared.common.generated.resources.Res
import voluntariatcv.shared.common.generated.resources.afternoon
import voluntariatcv.shared.common.generated.resources.all_day
import voluntariatcv.shared.common.generated.resources.dinner
import voluntariatcv.shared.common.generated.resources.general
import voluntariatcv.shared.common.generated.resources.ic_group
import voluntariatcv.shared.common.generated.resources.ic_lunch
import voluntariatcv.shared.common.generated.resources.ic_moon
import voluntariatcv.shared.common.generated.resources.ic_target
import voluntariatcv.shared.common.generated.resources.lunch
import voluntariatcv.shared.common.generated.resources.morning
import voluntariatcv.shared.common.generated.resources.specific

/*
@Composable
fun Shift.displayName(): String = when (this) {
    Shift.Morning -> stringResource(Res.string.morning)
    Shift.Afternoon -> stringResource(Res.string.afternoon)
    Shift.AllDay -> stringResource(Res.string.all_day)
    else -> ""
}
 */

@Composable
fun Meal.displayName() =
    when (this) {
        Meal.Lunch -> stringResource(Res.string.lunch)
        Meal.Dinner -> stringResource(Res.string.dinner)
        else -> ""
    }

@Composable
fun Meal.getBackgroundColor() =
    when (this) {
        Meal.Lunch -> MaterialTheme.colorScheme.primary
        Meal.Dinner -> Color(0xFF8FA399)
        else -> Color.Transparent
    }

@Composable
fun Meal.getIcon() =
    when (this) {
        Meal.Lunch -> painterResource(Res.drawable.ic_lunch)
        Meal.Dinner -> painterResource(Res.drawable.ic_moon)
        else -> null
    }

@Composable
fun VolunteerType.displayName() =
    when (this) {
        VolunteerType.General -> stringResource(Res.string.general)
        VolunteerType.Specific -> stringResource(Res.string.specific)
    }

@Composable
fun VolunteerType.getBackgroundColor() =
    when (this) {
        VolunteerType.General -> Color(0xFFC2A47D)
        VolunteerType.Specific -> Color(0xFF9E816E)
    }

@Composable
fun VolunteerType.getIcon() =
    when (this) {
        VolunteerType.General -> painterResource(Res.drawable.ic_group)
        VolunteerType.Specific -> painterResource(Res.drawable.ic_target)
    }