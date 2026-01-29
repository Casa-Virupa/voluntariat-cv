package com.casavirupa.voluntariat.features.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.casavirupa.voluntariat.features.authentication.password.ConfirmPasswordScreen
import com.casavirupa.voluntariat.features.authentication.signin.LogInScreen
import kotlinx.serialization.Serializable

@Serializable
data object SignInRoute

@Serializable
data object CreatePasswordRoute

fun NavController.navigateToCreatePassword() = navigate(CreatePasswordRoute)

fun NavGraphBuilder.authRoutes(
    onNavigateToCreatePassword: () -> Unit,
    onNavigateToSchedule: () -> Unit,
) {
    composable<SignInRoute> {
        LogInScreen(
            onNavigateToCreatePassword = onNavigateToCreatePassword,
            onNavigateToSchedule = onNavigateToSchedule,
        )
    }
    composable<CreatePasswordRoute> {
        ConfirmPasswordScreen(
            onNavigateToSchedule = onNavigateToSchedule,
        )
    }
}