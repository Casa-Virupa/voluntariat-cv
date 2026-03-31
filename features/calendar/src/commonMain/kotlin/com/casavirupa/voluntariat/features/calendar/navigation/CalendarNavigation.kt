package com.casavirupa.voluntariat.features.calendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.calendar.screens.CalendarScreen
import com.casavirupa.voluntariat.features.calendar.screens.ReservationFormScreen
import com.casavirupa.voluntariat.shared.core.navigation.MainNavigator
import kotlinx.serialization.Serializable

@Serializable
data object CalendarNavKey : NavKey

@Serializable
data object ReservationFormNavKey : NavKey

fun EntryProviderScope<NavKey>.calendarEntry(navigator: MainNavigator) {
    entry<CalendarNavKey> {
        CalendarScreen(
            onNavToReservationForm = { navigator.navigate(ReservationFormNavKey) },
        )
    }
    entry<ReservationFormNavKey> {
        ReservationFormScreen()
    }
}
