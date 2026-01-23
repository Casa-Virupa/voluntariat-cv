package com.casavirupa.voluntariat.shared.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.casavirupa.voluntariat.features.authentication.navigation.LogInRoute
import com.casavirupa.voluntariat.features.authentication.navigation.authRoutes
import com.casavirupa.voluntariat.features.authentication.navigation.navigateToConfirmPassword

@Composable
fun App() {
    val navController = rememberNavController()

    Scaffold {
        NavHost(
            navController = navController,
            startDestination = LogInRoute,
        ) {
            authRoutes(
                onNavToConfirmPassword = navController::navigateToConfirmPassword,
            )
        }
    }
}