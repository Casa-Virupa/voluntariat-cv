package com.casavirupa.voluntariat.features.calendar.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.casavirupa.voluntariat.shared.model.calendar.CalendarFilter

class CalendarFiltersViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val allFilters = CalendarFilter.entries.toList()

    val currentFilter = savedStateHandle.getStateFlow(
        key = FILTERS_KEY,
        initialValue = CalendarFilter.SeeAll,
    )

    fun selectFilter(filter: CalendarFilter) {
        if (filter == currentFilter.value) {
            return
        }
        savedStateHandle[FILTERS_KEY] = filter
    }

    companion object {
        private const val FILTERS_KEY = "calendar_filters_key"
    }
}