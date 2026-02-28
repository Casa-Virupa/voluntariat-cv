package com.casavirupa.voluntariat.features.authentication.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.casavirupa.voluntariat.features.authentication.password.CreatePasswordScreen
import com.casavirupa.voluntariat.features.authentication.signin.LogInScreen
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavKey
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json.Default.serializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data object SignInNavKey : AuthNavKey()

@Serializable
data object CreatePasswordNavKey : AuthNavKey()

@Serializable
data object MainAppContentNavKey : AuthNavKey(isLastNavKey = true)

fun NavController.navigateToCreatePassword() = navigate(CreatePasswordNavKey)

fun NavGraphBuilder.authRoutes(
    onNavigateToCreatePassword: () -> Unit,
    onNavigateToSchedule: () -> Unit,
) {
    composable<SignInNavKey> {
        LogInScreen(
            onNavigateToCreatePassword = onNavigateToCreatePassword,
            onNavigateToSchedule = onNavigateToSchedule,
        )
    }
    composable<CreatePasswordNavKey> {
        CreatePasswordScreen(
            onNavigateToSchedule = onNavigateToSchedule,
        )
    }
}

fun authNavigationConfig() = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SignInNavKey::class, SignInNavKey.serializer())
            subclass(CreatePasswordNavKey::class, CreatePasswordNavKey.serializer())
            subclass(MainAppContentNavKey::class, MainAppContentNavKey.serializer())
        }
    }
}

fun EntryProviderScope<NavKey>.authEntry(navigator: AuthNavigator) {
    entry<SignInNavKey> {
        LogInScreen(
            onNavigateToCreatePassword = { navigator.navigate(CreatePasswordNavKey) },
            onNavigateToSchedule = { navigator.navigate(MainAppContentNavKey) },
        )
    }
    entry<CreatePasswordNavKey> {
        CreatePasswordScreen(
            onNavigateToSchedule = { navigator.navigate(MainAppContentNavKey) }
        )
    }
}

// Kalice, DMSans