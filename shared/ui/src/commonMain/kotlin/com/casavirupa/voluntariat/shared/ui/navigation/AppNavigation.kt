package com.casavirupa.voluntariat.shared.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.authentication.navigation.SignInNavKey
import com.casavirupa.voluntariat.shared.core.navigation.AuthNavigator
import com.casavirupa.voluntariat.shared.core.navigation.MainAppContentNavKey
import com.casavirupa.voluntariat.shared.ui.MainApp

fun EntryProviderScope<NavKey>.rootMainContentEntry(authNavigator: AuthNavigator) {
    entry<MainAppContentNavKey> {
        MainApp(onLogOut = { authNavigator.navigate(SignInNavKey, clearStack = true) })
    }
}
