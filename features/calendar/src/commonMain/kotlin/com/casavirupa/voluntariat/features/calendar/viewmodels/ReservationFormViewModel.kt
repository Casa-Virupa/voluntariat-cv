package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class ReservationFormViewModel : ViewModel() {
    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()

    private val _sleep = MutableStateFlow(false)
    val sleep: StateFlow<Boolean> = _sleep.asStateFlow()

    private val _schedule = MutableStateFlow<ScheduleRange?>(null)
    val schedule: StateFlow<ScheduleRange?> = _schedule.asStateFlow()

    private val _technicalArea = MutableStateFlow<TechnicalAreaTurn?>(null)
    val technicalArea: StateFlow<TechnicalAreaTurn?> = _technicalArea.asStateFlow()

    private val _meal = MutableStateFlow<MealType?>(null)
    val meal: StateFlow<MealType?> = _meal.asStateFlow()

    fun onDateChanged(date: LocalDate) {
        _date.update { date }
    }

    fun onSleepChanged(sleep: Boolean) {
        _sleep.update { sleep }
    }

    fun onScheduleChanged(schedules: ScheduleRange) {
        _schedule.update { schedules }
    }

    fun onTechnicalAreaChanged(technicalArea: TechnicalAreaTurn) {
        _technicalArea.update { technicalArea }
    }

    fun onMealChanged(meal: MealType) {
        _meal.update { meal }
    }

    fun onConfirm() {
        viewModelScope.launch {

        }
    }
}

enum class ScheduleRange {
    Morning,
    Afternoon,
    AllDay,
}

enum class TechnicalAreaTurn {
    Morning,
    Afternoon,
}

enum class MealType {
    Breakfast,
    Lunch,
    Dinner,
}