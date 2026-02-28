package com.casavirupa.voluntariat.shared.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.casavirupa.voluntariat.features.authentication.navigation.CreatePasswordNavKey
import com.casavirupa.voluntariat.features.authentication.navigation.MainAppContentNavKey
import com.casavirupa.voluntariat.features.authentication.navigation.SignInNavKey
import com.casavirupa.voluntariat.features.authentication.navigation.authEntry
import com.casavirupa.voluntariat.features.authentication.navigation.authNavigationConfig
import com.casavirupa.voluntariat.shared.common.InitialUserState
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavigator
import com.casavirupa.voluntariat.shared.core.navigation.rememberAuthNavigationState
import com.casavirupa.voluntariat.shared.core.navigation.toEntries
import com.casavirupa.voluntariat.shared.ui.navigation.mainContentEntry

@Composable
fun RootApp(userState: InitialUserState) {
    val navigationState = rememberAuthNavigationState(
        startKey = userState.toStartDestination(),
        config = authNavigationConfig(),
    )
    val navigator = remember { AuthNavigator(navigationState) }
    val entryProvider = entryProvider {
        authEntry(navigator)
        mainContentEntry()
    }

    Scaffold { innerPadding ->
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = navigator::goBack,
            modifier = Modifier.padding(innerPadding),
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