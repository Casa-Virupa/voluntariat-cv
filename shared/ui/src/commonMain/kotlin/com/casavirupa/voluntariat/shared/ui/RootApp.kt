package com.casavirupa.voluntariat.shared.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.casavirupa.voluntariat.features.authentication.navigation.CreatePasswordNavKey
import com.casavirupa.voluntariat.features.authentication.navigation.SignInNavKey
import com.casavirupa.voluntariat.features.authentication.navigation.authEntry
import com.casavirupa.voluntariat.features.authentication.navigation.authNavigationConfig
import com.casavirupa.voluntariat.shared.common.InitialUserState
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavigator
import com.casavirupa.voluntariat.shared.core.navigation.MainAppContentNavKey
import com.casavirupa.voluntariat.shared.core.navigation.rememberAuthNavigationState
import com.casavirupa.voluntariat.shared.core.navigation.toEntries
import com.casavirupa.voluntariat.shared.designsystem.theme.VoluntariatCVTheme
import com.casavirupa.voluntariat.shared.ui.navigation.rootMainContentEntry

@Composable
fun RootApp(userState: InitialUserState = InitialUserState.NotLogged) {
    val navigationState = rememberAuthNavigationState(
        startKey = userState.toStartDestination(),
        config = authNavigationConfig(),
    )
    val navigator = remember { AuthNavigator(navigationState) }
    val entryProvider = entryProvider {
        authEntry(navigator)
        rootMainContentEntry(navigator)
    }

    VoluntariatCVTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = navigator::goBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(innerPadding),
            )
        }
    }
}

private fun InitialUserState.toStartDestination(): NavKey =
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