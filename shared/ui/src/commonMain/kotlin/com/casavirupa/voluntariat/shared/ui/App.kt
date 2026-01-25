package com.casavirupa.voluntariat.shared.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.casavirupa.voluntariat.features.authentication.navigation.ConfirmPasswordRoute
import com.casavirupa.voluntariat.features.authentication.navigation.LogInRoute
import com.casavirupa.voluntariat.features.authentication.navigation.authRoutes
import com.casavirupa.voluntariat.features.authentication.navigation.navigateToConfirmPassword
import com.casavirupa.voluntariat.features.schedule.navigation.navigateToSchedule
import com.casavirupa.voluntariat.features.schedule.navigation.scheduleRoutes
import com.casavirupa.voluntariat.shared.common.InitialUserState

@Composable
fun App(userState: InitialUserState) {
    val navController = rememberNavController()

    Scaffold {
        NavHost(
            navController = navController,
            startDestination = userState.toStartDestination(),
        ) {
            authRoutes(
                onNavToConfirmPassword = navController::navigateToConfirmPassword,
                onNavToSchedule = navController::navigateToSchedule,
            )
            scheduleRoutes()
        }
    }
}

private fun InitialUserState.toStartDestination() =
    when (this) {
        InitialUserState.LoggedIn -> ConfirmPasswordRoute
        InitialUserState.NotLogged -> LogInRoute
    }