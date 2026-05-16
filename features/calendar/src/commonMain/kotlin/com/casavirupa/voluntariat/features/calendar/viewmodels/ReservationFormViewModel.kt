package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.afternoon
import voluntariatcv.features.calendar.generated.resources.dinner
import voluntariatcv.features.calendar.generated.resources.ic_afternoon
import voluntariatcv.features.calendar.generated.resources.ic_lunch
import voluntariatcv.features.calendar.generated.resources.ic_moon
import voluntariatcv.features.calendar.generated.resources.ic_sleep_bed
import voluntariatcv.features.calendar.generated.resources.ic_sun
import voluntariatcv.features.calendar.generated.resources.lunch
import voluntariatcv.features.calendar.generated.resources.morning
import voluntariatcv.features.calendar.generated.resources.stay_to_sleep

class ReservationFormViewModel(
    private val authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReservationFormUiState())
    val uiState: StateFlow<ReservationFormUiState> = _uiState.asStateFlow()

    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()

    private val _shifts = MutableStateFlow<List<ShiftUi>>(emptyList())
    val shifts: StateFlow<List<ShiftUi>> = _shifts.asStateFlow()

    private val _additionalOptions = MutableStateFlow<List<AdditionalOption>>(emptyList())
    val additionalOptions: StateFlow<List<AdditionalOption>> = _additionalOptions.asStateFlow()

    fun onDateChanged(date: LocalDate) {
        _date.update { date }
    }

    fun onShiftChanged(shift: ShiftUi) {
        _shifts.update {
            val mutableShifts = it.toMutableList()
            if (mutableShifts.contains(shift)) {
                mutableShifts.remove(shift)
            } else {
                mutableShifts.add(shift)
            }
            mutableShifts.toList()
        }
    }

    fun onAdditionOptionSelected(option: AdditionalOption) {
        _additionalOptions.update {
            val mutableOptions = it.toMutableList()
            if (mutableOptions.contains(option)) {
                mutableOptions.remove(option)
            } else {
                mutableOptions.add(option)
            }
            mutableOptions.toList()
        }
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
        date.value != null && shifts.value.isNotEmpty()
    private fun buildReservation() =
        Volunteer(
            id = VolunteerId.Empty,
            userId = UserId.Empty,
            date = date.value!!,
            shift = Shift.Unknown,
            meals = additionalOptions.value.getMeals(),
            sleep = additionalOptions.value.contains(AdditionalOption.Sleep),
        )

    private fun navigateBack() {
        _uiState.update { it.copy(isFormSavedSuccessfully = true) }
    }
}

data class ReservationFormUiState(
    val isFormSavedSuccessfully: Boolean = false,
)

enum class ShiftUi(
    val text: StringResource,
    val icon: DrawableResource,
) {
    Morning(
        text = Res.string.morning,
        icon = Res.drawable.ic_sun,
    ),
    Afternoon(
        text = Res.string.afternoon,
        icon = Res.drawable.ic_afternoon,
    ),
}

enum class AdditionalOption(
    val text: StringResource,
    val icon: DrawableResource,
) {
    Lunch(
        text = Res.string.lunch,
        icon = Res.drawable.ic_lunch,
    ),
    Dinner(
        text = Res.string.dinner,
        icon = Res.drawable.ic_moon,
    ),
    Sleep(
        text = Res.string.stay_to_sleep,
        icon = Res.drawable.ic_sleep_bed,
    ),
}

private fun List<AdditionalOption>.getMeals() =
    filter { it != AdditionalOption.Sleep }.map(AdditionalOption::toMeal)

private fun AdditionalOption.toMeal() =
    when (this) {
        AdditionalOption.Lunch -> Meal.Lunch
        AdditionalOption.Dinner -> Meal.Dinner
        else -> Meal.Unknown
    }

private const val LOG_TAG = "ReservationFormViewModel"