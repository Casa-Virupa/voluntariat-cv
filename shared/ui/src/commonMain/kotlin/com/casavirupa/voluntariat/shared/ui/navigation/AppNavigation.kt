package com.casavirupa.voluntariat.shared.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.shared.core.navigation.AuthRootNavKey
import com.casavirupa.voluntariat.shared.core.navigation.MainAppContentNavKey
import com.casavirupa.voluntariat.shared.ui.MainApp
import com.casavirupa.voluntariat.shared.ui.RootApp

fun EntryProviderScope<NavKey>.rootMainContentEntry() {
    entry<MainAppContentNavKey> {
        MainApp()
    }
    entry<AuthRootNavKey> {
        RootApp()
    }
}
