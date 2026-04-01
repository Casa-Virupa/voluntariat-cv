package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class ReservationFormViewModel : ViewModel() {
    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()

    private val _sleep = MutableStateFlow(false)
    val sleep: StateFlow<Boolean> = _sleep.asStateFlow()

    private val _schedules = MutableStateFlow<List<LocalTime>>(emptyList())
    val schedules: StateFlow<List<LocalTime>> = _schedules.asStateFlow()

    private val _technicalArea = MutableStateFlow<TechnicalAreaTurn?>(null)
    val technicalArea: StateFlow<TechnicalAreaTurn?> = _technicalArea.asStateFlow()

    private val _meal = MutableStateFlow<MealType?>(null)
    val meal: StateFlow<MealType?> = _meal.asStateFlow()

    fun onConfirm() {
        viewModelScope.launch {

        }
    }
}

enum class TechnicalAreaTurn {
    Morning,
    Afternoon,
    Night,
}

enum class MealType {
    Breakfast,
    Lunch,
    Dinner,
}