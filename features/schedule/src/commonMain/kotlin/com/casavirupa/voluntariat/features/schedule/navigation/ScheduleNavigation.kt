package com.casavirupa.voluntariat.features.schedule.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.schedule.ScheduleScreen
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavigator
import com.casavirupa.voluntariat.shared.core.navigation.MainNavigator
import kotlinx.serialization.Serializable

@Serializable
data object ScheduleNavKey : NavKey

fun NavController.navigateToSchedule() = navigate(ScheduleNavKey)

fun NavGraphBuilder.scheduleRoutes() {
    composable<ScheduleNavKey> {
        ScheduleScreen()
    }
}

fun EntryProviderScope<NavKey>.scheduleEntry(navigator: MainNavigator) {
    entry<ScheduleNavKey> {
        ScheduleScreen()
    }
}
