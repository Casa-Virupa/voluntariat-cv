package com.casavirupa.voluntariat.features.calendar.screens

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CalendarFiltersViewModel : ViewModel() {
    val text = MutableStateFlow("Test").asStateFlow()

    init {
        Logger.d("asdd") { "Entered to CalendarFiltersViewModel" }
    }

    override fun onCleared() {
        Logger.d("asdd") { "Exit from CalendarFiltersViewModel" }
    }
}