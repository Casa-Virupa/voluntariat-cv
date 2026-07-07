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
import voluntariatcv.shared.common.generated.resources.area_animals
import voluntariatcv.shared.common.generated.resources.area_can_bordoi_events
import voluntariatcv.shared.common.generated.resources.area_communication
import voluntariatcv.shared.common.generated.resources.area_community_health
import voluntariatcv.shared.common.generated.resources.area_exterior
import voluntariatcv.shared.common.generated.resources.area_gardening
import voluntariatcv.shared.common.generated.resources.area_grants
import voluntariatcv.shared.common.generated.resources.area_graphical_design
import voluntariatcv.shared.common.generated.resources.area_grove
import voluntariatcv.shared.common.generated.resources.area_kitchen
import voluntariatcv.shared.common.generated.resources.area_labor
import voluntariatcv.shared.common.generated.resources.area_maintenance
import voluntariatcv.shared.common.generated.resources.area_pedagogical
import voluntariatcv.shared.common.generated.resources.area_registrations
import voluntariatcv.shared.common.generated.resources.area_shop
import voluntariatcv.shared.common.generated.resources.area_technical_audiovisual
import voluntariatcv.shared.common.generated.resources.area_technical_texts
import voluntariatcv.shared.common.generated.resources.area_temple
import voluntariatcv.shared.common.generated.resources.area_transcriptions
import voluntariatcv.shared.common.generated.resources.area_virupa_editions
import voluntariatcv.shared.common.generated.resources.area_volunteer_coordination
import voluntariatcv.shared.common.generated.resources.dinner
import voluntariatcv.shared.common.generated.resources.general
import voluntariatcv.shared.common.generated.resources.ic_group
import voluntariatcv.shared.common.generated.resources.ic_lunch
import voluntariatcv.shared.common.generated.resources.ic_moon
import voluntariatcv.shared.common.generated.resources.ic_target
import voluntariatcv.shared.common.generated.resources.lunch
import voluntariatcv.shared.common.generated.resources.specific

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
        SpecificArea.Animals -> stringResource(Res.string.area_animals)
        SpecificArea.Shop -> stringResource(Res.string.area_shop)
        SpecificArea.Communication -> stringResource(Res.string.area_communication)
        SpecificArea.VolunteerCoordination -> stringResource(Res.string.area_volunteer_coordination)
        SpecificArea.Kitchen -> stringResource(Res.string.area_kitchen)
        SpecificArea.GraphicalDesign -> stringResource(Res.string.area_graphical_design)
        SpecificArea.VirupaEditions -> stringResource(Res.string.area_virupa_editions)
        SpecificArea.Exterior -> stringResource(Res.string.area_exterior)
        SpecificArea.CanBordoiEvents -> stringResource(Res.string.area_can_bordoi_events)
        SpecificArea.Grove -> stringResource(Res.string.area_grove)
        SpecificArea.Registrations -> stringResource(Res.string.area_registrations)
        SpecificArea.Gardening -> stringResource(Res.string.area_gardening)
        SpecificArea.Labor -> stringResource(Res.string.area_labor)
        SpecificArea.Maintenance -> stringResource(Res.string.area_maintenance)
        SpecificArea.Pedagogical -> stringResource(Res.string.area_pedagogical)
        SpecificArea.CommunityHealth -> stringResource(Res.string.area_community_health)
        SpecificArea.Grants -> stringResource(Res.string.area_grants)
        SpecificArea.Temple -> stringResource(Res.string.area_temple)
        SpecificArea.Transcriptions -> stringResource(Res.string.area_transcriptions)
        SpecificArea.TechnicalAndAudiovisual -> stringResource(Res.string.area_technical_audiovisual)
        SpecificArea.TechnicalAndTexts -> stringResource(Res.string.area_technical_texts)
        SpecificArea.Unknown -> ""
    }