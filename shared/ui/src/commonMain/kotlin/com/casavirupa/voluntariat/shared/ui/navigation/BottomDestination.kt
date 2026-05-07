package com.casavirupa.voluntariat.shared.ui.navigation

import com.casavirupa.voluntariat.features.calendar.navigation.CalendarNavKey
import com.casavirupa.voluntariat.features.history.navigation.HistoryNavKey
import com.casavirupa.voluntariat.shared.core.navigation.MainNavKey

enum class BottomDestination(
    val destinationKey: MainNavKey
) {
    Calendar(
        destinationKey = CalendarNavKey
    ),
    History(
        destinationKey = HistoryNavKey
    )
}