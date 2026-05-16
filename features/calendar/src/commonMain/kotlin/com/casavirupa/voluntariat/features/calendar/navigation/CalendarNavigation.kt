package com.casavirupa.voluntariat.features.calendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.calendar.screens.CalendarScreen
import com.casavirupa.voluntariat.features.calendar.screens.DayDetailScreen
import com.casavirupa.voluntariat.features.calendar.screens.ReservationFormScreen
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayDetailViewModel
import com.casavirupa.voluntariat.shared.core.navigation.MainNavKey
import com.casavirupa.voluntariat.shared.core.navigation.MainNavigator
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object CalendarNavKey : MainNavKey(showNavigationBar = true)

@Serializable
data object ReservationFormNavKey : MainNavKey()

@Serializable
data class DayDetailNavKey(val date: LocalDate) : MainNavKey()

fun EntryProviderScope<NavKey>.calendarEntry(navigator: MainNavigator) {
    entry<CalendarNavKey> {
        CalendarScreen(
            onNavToReservationForm = { navigator.navigate(ReservationFormNavKey) },
            onDayClick = { date -> navigator.navigate(DayDetailNavKey(date)) }
        )
    }
    entry<ReservationFormNavKey> {
        ReservationFormScreen(
            onNavBack = { navigator.goBack() },
        )
    }
    entry<DayDetailNavKey> { key ->
        val viewModel = koinViewModel<DayDetailViewModel> { parametersOf(key) }
        DayDetailScreen(
            onNavBack = { navigator.goBack() },
            viewModel = viewModel,
        )
    }
}
