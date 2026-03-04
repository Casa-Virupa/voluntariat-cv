package com.casavirupa.voluntariat.features.calendar.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.calendar.CalendarScreen
import com.casavirupa.voluntariat.shared.core.navigation.MainNavigator
import kotlinx.serialization.Serializable

@Serializable
data object CalendarNavKey : NavKey

fun EntryProviderScope<NavKey>.calendarEntry(navigator: MainNavigator) {
    entry<CalendarNavKey> {
        CalendarScreen()
    }
}
