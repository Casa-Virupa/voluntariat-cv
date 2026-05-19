package com.casavirupa.voluntariat.features.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.profile.ProfileScreen
import com.casavirupa.voluntariat.shared.core.navigation.MainNavKey
import kotlinx.serialization.Serializable

@Serializable
data object ProfileNavKey : MainNavKey(showNavigationBar = true)

fun EntryProviderScope<NavKey>.profileEntry(onLogOut: () -> Unit) {
    entry<ProfileNavKey> {
        ProfileScreen(onNavigateToSignIn = onLogOut)
    }
}
