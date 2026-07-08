package com.casavirupa.voluntariat.features.authentication.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.casavirupa.voluntariat.features.authentication.password.CreatePasswordScreen
import com.casavirupa.voluntariat.features.authentication.password.ForgotPasswordScreen
import com.casavirupa.voluntariat.features.authentication.signin.SignInScreen
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavKey
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavigator
import com.casavirupa.voluntariat.shared.core.navigation.MainAppContentNavKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data object SignInNavKey : AuthNavKey()

@Serializable
data object CreatePasswordNavKey : AuthNavKey()

@Serializable
data object ForgotPasswordNavKey : AuthNavKey()

fun authNavigationConfig() = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(SignInNavKey::class, SignInNavKey.serializer())
            subclass(CreatePasswordNavKey::class, CreatePasswordNavKey.serializer())
            subclass(MainAppContentNavKey::class, MainAppContentNavKey.serializer())
            subclass(ForgotPasswordNavKey::class, ForgotPasswordNavKey.serializer())
        }
    }
}

fun EntryProviderScope<NavKey>.authEntry(navigator: AuthNavigator) {
    entry<SignInNavKey> {
        SignInScreen(
            onNavigateToCreatePassword = { navigator.navigate(CreatePasswordNavKey) },
            onNavigateToSchedule = { navigator.navigate(MainAppContentNavKey) },
            onNavigateToForgotPassword = { navigator.navigate(ForgotPasswordNavKey) }
        )
    }
    entry<CreatePasswordNavKey> {
        CreatePasswordScreen(
            onNavigateToSchedule = { navigator.navigate(MainAppContentNavKey) }
        )
    }
    entry<ForgotPasswordNavKey> {
        ForgotPasswordScreen(
            onNavBack = { navigator.goBack() }
        )
    }
}
