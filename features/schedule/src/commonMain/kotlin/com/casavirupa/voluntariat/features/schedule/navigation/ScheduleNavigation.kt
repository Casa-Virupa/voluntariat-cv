package com.casavirupa.voluntariat.features.schedule.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.casavirupa.voluntariat.features.schedule.ScheduleScreen
import kotlinx.serialization.Serializable

@Serializable
data object ScheduleRoute

fun NavController.navigateToSchedule() = navigate(ScheduleRoute)

fun NavGraphBuilder.scheduleRoutes() {
    composable<ScheduleRoute> {
        ScheduleScreen()
    }
}
