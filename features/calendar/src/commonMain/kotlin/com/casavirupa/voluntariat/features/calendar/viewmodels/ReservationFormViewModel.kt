package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Reservation
import com.casavirupa.voluntariat.shared.model.calendar.ReservationId
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerShift
import com.casavirupa.voluntariat.shared.model.calendar.TechnicalAreaTurn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class ReservationFormViewModel(
    private val authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReservationFormUiState())
    val uiState: StateFlow<ReservationFormUiState> = _uiState.asStateFlow()

    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()

    private val _sleep = MutableStateFlow(false)
    val sleep: StateFlow<Boolean> = _sleep.asStateFlow()

    private val _schedule = MutableStateFlow<VolunteerShift?>(null)
    val schedule: StateFlow<VolunteerShift?> = _schedule.asStateFlow()

    private val _technicalArea = MutableStateFlow<TechnicalAreaTurn?>(null)
    val technicalArea: StateFlow<TechnicalAreaTurn?> = _technicalArea.asStateFlow()

    private val _meal = MutableStateFlow<Meal?>(null)
    val meal: StateFlow<Meal?> = _meal.asStateFlow()

    fun onDateChanged(date: LocalDate) {
        _date.update { date }
    }

    fun onSleepChanged(sleep: Boolean) {
        _sleep.update { sleep }
    }

    fun onScheduleChanged(schedules: VolunteerShift) {
        _schedule.update { schedules }
    }

    fun onTechnicalAreaChanged(technicalArea: TechnicalAreaTurn) {
        _technicalArea.update { technicalArea }
    }

    fun onMealChanged(meal: Meal) {
        _meal.update { meal }
    }

    fun onConfirm() {
        if (!formInputsAreValid()) {
            // TODO: Show error
            return
        }
        viewModelScope.launch {
            authRepository
                .getCurrentUser()
                .onSuccess { user ->
                    Logger.d("asdd") { user.id.toString() }
                    calendarRepository
                        .reserveDay(user.id, buildReservation())
                        .onSuccess {
                            navigateBack()
                        }.onFailure {
                            // TODO: Show error
                            Logger.d(LOG_TAG) { "Error reserving a day" }
                        }
                }.onFailure {
                    // TODO: Show error
                    Logger.d(LOG_TAG) { "Error getting current user when reserving a day" }
                }
        }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(isFormSavedSuccessfully = false) }
    }

    private fun formInputsAreValid() =
        date.value != null &&
            schedule.value != null &&
            technicalArea.value != null &&
            meal.value != null

    private fun buildReservation() =
        Reservation(
            id = ReservationId(""),
            date = date.value!!,
            volunteerShift = schedule.value!!,
            technicalAreaTurn = technicalArea.value!!,
            meal = meal.value!!,
            sleep = sleep.value,
        )

    private fun navigateBack() {
        _uiState.update { it.copy(isFormSavedSuccessfully = true) }
    }
}

data class ReservationFormUiState(
    val isFormSavedSuccessfully: Boolean = false,
)

private const val LOG_TAG = "ReservationFormViewModel"