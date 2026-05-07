package com.casavirupa.voluntariat.features.history.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.history.HistoryScreen
import com.casavirupa.voluntariat.shared.core.navigation.MainNavKey
import com.casavirupa.voluntariat.shared.core.navigation.MainNavigator
import kotlinx.serialization.Serializable

@Serializable
data object HistoryNavKey : MainNavKey(showNavigationBar = true)

fun EntryProviderScope<NavKey>.historyEntry(navigator: MainNavigator) {
    entry<HistoryNavKey> {
        HistoryScreen()
    }
}