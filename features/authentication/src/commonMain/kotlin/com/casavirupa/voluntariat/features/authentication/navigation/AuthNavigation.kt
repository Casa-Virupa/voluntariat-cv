package com.casavirupa.voluntariat.features.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.casavirupa.voluntariat.features.authentication.confirmation.ConfirmPasswordScreen
import com.casavirupa.voluntariat.features.authentication.login.LogInScreen
import kotlinx.serialization.Serializable

@Serializable
data object LogInRoute

@Serializable
data object CreatePasswordRoute

fun NavController.navigateToConfirmPassword() = navigate(CreatePasswordRoute)

fun NavGraphBuilder.authRoutes(
    onNavToConfirmPassword: () -> Unit,
    onNavToSchedule: () -> Unit,
) {
    composable<LogInRoute> {
        LogInScreen(
            onLogIn = onNavToConfirmPassword,
        )
    }
    composable<CreatePasswordRoute> {
        ConfirmPasswordScreen(
            onCreatePassword = onNavToSchedule,
        )
    }
}