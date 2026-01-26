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

fun NavController.navigateToConfirmPassword() = navigate(CreatePasswordRoute)

fun NavGraphBuilder.authRoutes(
    onNavToConfirmPassword: () -> Unit,
    onNavToSchedule: () -> Unit,
) {
    composable<SignInRoute> {
        LogInScreen(
            onSignIn = onNavToConfirmPassword,
        )
    }
    composable<CreatePasswordRoute> {
        ConfirmPasswordScreen(
            onCreatePassword = onNavToSchedule,
        )
    }
}