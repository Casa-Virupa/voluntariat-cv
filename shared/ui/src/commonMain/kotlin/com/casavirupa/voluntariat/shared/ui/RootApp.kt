package com.casavirupa.voluntariat.shared.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.casavirupa.voluntariat.features.authentication.navigation.CreatePasswordNavKey
import com.casavirupa.voluntariat.features.authentication.navigation.MainAppContentNavKey
import com.casavirupa.voluntariat.features.authentication.navigation.SignInNavKey
import com.casavirupa.voluntariat.features.authentication.navigation.authEntry
import com.casavirupa.voluntariat.features.authentication.navigation.authNavigationConfig
import com.casavirupa.voluntariat.features.authentication.navigation.authRoutes
import com.casavirupa.voluntariat.features.authentication.navigation.navigateToCreatePassword
import com.casavirupa.voluntariat.features.schedule.navigation.navigateToSchedule
import com.casavirupa.voluntariat.features.schedule.navigation.scheduleRoutes
import com.casavirupa.voluntariat.shared.common.InitialUserState
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavigator
import com.casavirupa.voluntariat.shared.core.navigation.rememberAuthNavigationState
import com.casavirupa.voluntariat.shared.core.navigation.toEntries
import com.casavirupa.voluntariat.shared.ui.navigation.mainContentEntry

@Composable
fun RootApp(userState: InitialUserState) {
    val authNavigationState = rememberAuthNavigationState(
        startKey = userState.toStartDestination(),
        config = authNavigationConfig(),
    )
    val authNavigator = remember { AuthNavigator(authNavigationState) }
    val entryProvider = entryProvider {
        authEntry(authNavigator)
        mainContentEntry()
    }

    Scaffold {
        NavDisplay(
            entries = authNavigationState.toEntries(entryProvider),
            onBack = { authNavigator.goBack() },
        )
    }
}

private fun InitialUserState.toStartDestination() =
    when (this) {
        is InitialUserState.LoggedIn -> {
            if (onboardingCompleted) {
                MainAppContentNavKey
            } else {
                CreatePasswordNavKey
            }
        }
        InitialUserState.NotLogged -> SignInNavKey
    }