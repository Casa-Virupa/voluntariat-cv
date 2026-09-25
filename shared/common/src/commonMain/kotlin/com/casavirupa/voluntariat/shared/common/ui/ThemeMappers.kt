package com.casavirupa.voluntariat.shared.common.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import voluntariatcv.shared.common.generated.resources.Res
import voluntariatcv.shared.common.generated.resources.area_amrita
import voluntariatcv.shared.common.generated.resources.area_animals
import voluntariatcv.shared.common.generated.resources.area_audiovisual
import voluntariatcv.shared.common.generated.resources.area_communication
import voluntariatcv.shared.common.generated.resources.area_exteriors_gardening
import voluntariatcv.shared.common.generated.resources.area_grants
import voluntariatcv.shared.common.generated.resources.area_graphical_design
import voluntariatcv.shared.common.generated.resources.area_grove
import voluntariatcv.shared.common.generated.resources.area_health
import voluntariatcv.shared.common.generated.resources.area_kitchen
import voluntariatcv.shared.common.generated.resources.area_labor
import voluntariatcv.shared.common.generated.resources.area_maintenance
import voluntariatcv.shared.common.generated.resources.area_pedagogical
import voluntariatcv.shared.common.generated.resources.area_registrations
import voluntariatcv.shared.common.generated.resources.area_shop
import voluntariatcv.shared.common.generated.resources.area_technical_programming
import voluntariatcv.shared.common.generated.resources.area_temple
import voluntariatcv.shared.common.generated.resources.area_texts
import voluntariatcv.shared.common.generated.resources.area_transcriptions
import voluntariatcv.shared.common.generated.resources.area_virupa_editions
import voluntariatcv.shared.common.generated.resources.area_volunteer_coordination
import voluntariatcv.shared.common.generated.resources.area_works
import voluntariatcv.shared.common.generated.resources.breakfast
import voluntariatcv.shared.common.generated.resources.breakfast_next_day
import voluntariatcv.shared.common.generated.resources.dinner
import voluntariatcv.shared.common.generated.resources.general
import voluntariatcv.shared.common.generated.resources.ic_breakfast
import voluntariatcv.shared.common.generated.resources.ic_group
import voluntariatcv.shared.common.generated.resources.ic_lunch
import voluntariatcv.shared.common.generated.resources.ic_moon
import voluntariatcv.shared.common.generated.resources.ic_target
import voluntariatcv.shared.common.generated.resources.lunch
import voluntariatcv.shared.common.generated.resources.specific

@Composable
fun Meal.displayName() =
    when (this) {
        Meal.Breakfast -> stringResource(Res.string.breakfast)
        Meal.BreakfastNextDay -> stringResource(Res.string.breakfast_next_day)
        Meal.Lunch -> stringResource(Res.string.lunch)
        Meal.Dinner -> stringResource(Res.string.dinner)
        else -> ""
    }

@Composable
fun Meal.getBackgroundColor() =
    when (this) {
        Meal.Breakfast -> Color(0xFFBC8F5E)
        Meal.BreakfastNextDay -> Color(0xFFA67C52)
        Meal.Lunch -> MaterialTheme.colorScheme.primary
        Meal.Dinner -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }

@Composable
fun Meal.getIcon() =
    when (this) {
        Meal.Breakfast -> painterResource(Res.drawable.ic_breakfast)
        Meal.BreakfastNextDay -> painterResource(Res.drawable.ic_breakfast)
        Meal.Lunch -> painterResource(Res.drawable.ic_lunch)
        Meal.Dinner -> painterResource(Res.drawable.ic_moon)
        else -> null
    }

@Composable
fun VolunteerType.displayName() =
    when (this) {
        VolunteerType.General -> stringResource(Res.string.general)
        is VolunteerType.Specific -> {
            "${stringResource(Res.string.specific)} · ${specificArea.displayName()}"
        }
    }

@Composable
fun VolunteerType.getBackgroundColor() =
    when (this) {
        VolunteerType.General -> Color(0xFFC2A47D)
        is VolunteerType.Specific -> Color(0xFF9E816E)
    }

@Composable
fun VolunteerType.getIcon() =
    when (this) {
        VolunteerType.General -> painterResource(Res.drawable.ic_group)
        is VolunteerType.Specific -> painterResource(Res.drawable.ic_target)
    }

@Composable
fun SpecificArea.displayName() =
    when (this) {
        SpecificArea.Amrita -> stringResource(Res.string.area_amrita)
        SpecificArea.Animals -> stringResource(Res.string.area_animals)
        SpecificArea.Shop -> stringResource(Res.string.area_shop)
        SpecificArea.Audiovisual -> stringResource(Res.string.area_audiovisual)
        SpecificArea.Communication -> stringResource(Res.string.area_communication)
        SpecificArea.VolunteerCoordination -> stringResource(Res.string.area_volunteer_coordination)
        SpecificArea.Kitchen -> stringResource(Res.string.area_kitchen)
        SpecificArea.GraphicalDesign -> stringResource(Res.string.area_graphical_design)
        SpecificArea.VirupaEditions -> stringResource(Res.string.area_virupa_editions)
        SpecificArea.ExteriorsAndGardening -> stringResource(Res.string.area_exteriors_gardening)
        SpecificArea.Grove -> stringResource(Res.string.area_grove)
        SpecificArea.Registrations -> stringResource(Res.string.area_registrations)
        SpecificArea.Labor -> stringResource(Res.string.area_labor)
        SpecificArea.Maintenance -> stringResource(Res.string.area_maintenance)
        SpecificArea.Works -> stringResource(Res.string.area_works)
        SpecificArea.Pedagogical -> stringResource(Res.string.area_pedagogical)
        SpecificArea.Health -> stringResource(Res.string.area_health)
        SpecificArea.Grants -> stringResource(Res.string.area_grants)
        SpecificArea.TechnicalAndProgramming -> stringResource(Res.string.area_technical_programming)
        SpecificArea.Temple -> stringResource(Res.string.area_temple)
        SpecificArea.Texts -> stringResource(Res.string.area_texts)
        SpecificArea.Transcriptions -> stringResource(Res.string.area_transcriptions)
        SpecificArea.Unknown -> ""
    }