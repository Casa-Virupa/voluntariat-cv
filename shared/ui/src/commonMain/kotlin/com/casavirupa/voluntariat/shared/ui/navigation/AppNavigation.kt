package com.casavirupa.voluntariat.shared.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.authentication.navigation.MainAppContentNavKey
import com.casavirupa.voluntariat.shared.ui.MainApp

fun EntryProviderScope<NavKey>.mainContentEntry() {
    entry<MainAppContentNavKey> {
        MainApp()
    }
}
