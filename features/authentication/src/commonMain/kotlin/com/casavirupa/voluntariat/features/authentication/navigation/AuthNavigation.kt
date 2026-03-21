package com.casavirupa.voluntariat.features.authentication.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.casavirupa.voluntariat.features.authentication.password.CreatePasswordScreen
import com.casavirupa.voluntariat.features.authentication.signin.SignInScreen
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavKey
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavigator
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data object SignInNavKey : AuthNavKey()

@Serializable
data object CreatePasswordNavKey : AuthNavKey()

@Serializable
data object MainAppContentNavKey : AuthNavKey(isLastNavKey = true)

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
        SignInScreen(
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
