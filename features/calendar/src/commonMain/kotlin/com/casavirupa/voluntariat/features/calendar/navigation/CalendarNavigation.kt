package com.casavirupa.voluntariat.features.calendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.casavirupa.voluntariat.features.calendar.screens.CalendarScreen
import com.casavirupa.voluntariat.features.calendar.screens.DayDetailScreen
import com.casavirupa.voluntariat.features.calendar.screens.ReservationFormScreen
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayDetailViewModel
import com.casavirupa.voluntariat.features.calendar.viewmodels.ReservationFormViewModel
import com.casavirupa.voluntariat.shared.core.navigation.MainNavKey
import com.casavirupa.voluntariat.shared.core.navigation.MainNavigator
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object CalendarNavKey : MainNavKey(showNavigationBar = true)

/**
 * [initialDate] pre-fills the form's date when the form is opened from a day's detail; null
 * (from the calendar FAB) leaves the date empty for the user to pick.
 */
@Serializable
data class ReservationFormNavKey(val initialDate: LocalDate? = null) : MainNavKey()

@Serializable
data class DayDetailNavKey(val date: LocalDate) : MainNavKey()

fun EntryProviderScope<NavKey>.calendarEntry(navigator: MainNavigator) {
    entry<CalendarNavKey> {
        CalendarScreen(
            onNavToReservationForm = { navigator.navigate(ReservationFormNavKey()) },
            onDayClick = { date -> navigator.navigate(DayDetailNavKey(date)) }
        )
    }
    entry<ReservationFormNavKey> { key ->
        val viewModel = koinViewModel<ReservationFormViewModel> { parametersOf(key) }
        ReservationFormScreen(
            onNavBack = { navigator.goBack() },
            // Whether opened from the calendar or from a day's detail, a saved booking lands
            // on the calendar: the day detail underneath would otherwise show stale data.
            onSaved = { navigator.popTo(CalendarNavKey) },
            viewModel = viewModel,
        )
    }
    entry<DayDetailNavKey> { key ->
        val viewModel = koinViewModel<DayDetailViewModel> { parametersOf(key) }
        DayDetailScreen(
            onNavBack = { navigator.goBack() },
            onNavToReservationForm = {
                navigator.navigate(ReservationFormNavKey(initialDate = key.date))
            },
            viewModel = viewModel,
        )
    }
}
