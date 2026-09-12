package com.casavirupa.voluntariat.shared.ui.navigation

import com.casavirupa.voluntariat.features.calendar.navigation.CalendarNavKey
import com.casavirupa.voluntariat.features.history.navigation.HistoryNavKey
import com.casavirupa.voluntariat.features.profile.navigation.ProfileNavKey
import com.casavirupa.voluntariat.shared.core.navigation.MainNavKey
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import voluntariatcv.shared.ui.generated.resources.Res
import voluntariatcv.shared.ui.generated.resources.calendar_menu
import voluntariatcv.shared.ui.generated.resources.history_menu
import voluntariatcv.shared.ui.generated.resources.ic_calendar_today
import voluntariatcv.shared.ui.generated.resources.ic_history
import voluntariatcv.shared.ui.generated.resources.ic_person
import voluntariatcv.shared.ui.generated.resources.profile_menu

enum class BottomDestination(
    val navKey: MainNavKey,
    val icon: DrawableResource,
    val label: StringResource,
) {
    Calendar(
        navKey = CalendarNavKey,
        icon = Res.drawable.ic_calendar_today,
        label = Res.string.calendar_menu,
    ),
    History(
        navKey = HistoryNavKey,
        icon = Res.drawable.ic_history,
        label = Res.string.history_menu,
    ),
    Profile(
        navKey = ProfileNavKey,
        icon = Res.drawable.ic_person,
        label = Res.string.profile_menu,
    )
}
