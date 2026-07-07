package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.shared.core.utils.Throttler
import com.casavirupa.voluntariat.shared.core.utils.getFirstDayOfMonth
import com.casavirupa.voluntariat.shared.core.utils.getLastDayOfMonth
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.PaymentRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.afternoon
import voluntariatcv.features.calendar.generated.resources.dinner
import voluntariatcv.features.calendar.generated.resources.general
import voluntariatcv.features.calendar.generated.resources.ic_afternoon
import voluntariatcv.features.calendar.generated.resources.ic_group
import voluntariatcv.features.calendar.generated.resources.ic_lunch
import voluntariatcv.features.calendar.generated.resources.ic_moon
import voluntariatcv.features.calendar.generated.resources.ic_sleep_bed
import voluntariatcv.features.calendar.generated.resources.ic_sun
import voluntariatcv.features.calendar.generated.resources.ic_target
import voluntariatcv.features.calendar.generated.resources.lunch
import voluntariatcv.features.calendar.generated.resources.morning
import voluntariatcv.features.calendar.generated.resources.specific
import voluntariatcv.features.calendar.generated.resources.stay_to_sleep
import kotlin.time.Clock

class ReservationFormViewModel(
    private val authRepository: AuthRepository,
    private val volunteerRepository: VolunteerRepository,
    private val calendarRepository: CalendarRepository,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {
    private val throttler = Throttler()

    private val _uiState = MutableStateFlow(ReservationFormUiState())
    val uiState: StateFlow<ReservationFormUiState> = _uiState.asStateFlow()

    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()

    private val _shifts = MutableStateFlow<List<ShiftUi>>(emptyList())
    val shifts: StateFlow<List<ShiftUi>> = _shifts.asStateFlow()

    private val _additionalOptions = MutableStateFlow<List<AdditionalOption>>(emptyList())
    val additionalOptions: StateFlow<List<AdditionalOption>> = _additionalOptions.asStateFlow()

    private val _shownModal = MutableStateFlow(ShownModal.None)
    val shownModal: StateFlow<ShownModal> = _shownModal.asStateFlow()

    private val _morningVolunteerType = MutableStateFlow(FormVolunteerTypeUi.General)
    val morningVolunteerType: StateFlow<FormVolunteerTypeUi> = _morningVolunteerType.asStateFlow()

    private val _afternoonVolunteerType = MutableStateFlow(FormVolunteerTypeUi.General)
    val afternoonVolunteerType: StateFlow<FormVolunteerTypeUi> =
        _afternoonVolunteerType.asStateFlow()

    private val _morningTimeRange = MutableStateFlow(TimeRange.DefaultMorning)
    val morningTimeRange: StateFlow<TimeRange> = _morningTimeRange.asStateFlow()

    private val _afternoonTimeRange = MutableStateFlow(TimeRange.DefaultAfternoon)
    val afternoonTimeRange: StateFlow<TimeRange> = _afternoonTimeRange.asStateFlow()

    private val _shiftsInfo = MutableStateFlow<List<ShiftInfoSummary>>(emptyList())
    val shiftsInfo: StateFlow<List<ShiftInfoSummary>> = _shiftsInfo.asStateFlow()

    private val _showExistingVolunteerDialogError = MutableStateFlow(false)
    val showExistingVolunteerDialogError: StateFlow<Boolean> =
        _showExistingVolunteerDialogError.asStateFlow()

    val specificAreas: StateFlow<List<SpecificArea>> =
        authRepository
            .getCurrentUserFlow()
            .map { it.specificAreas }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyList(),
            )

    val showSpecificAreaSelector: StateFlow<Boolean> =
        combine(
            morningVolunteerType,
            afternoonVolunteerType,
            specificAreas,
            shownModal,
        ) { morningVolunteerType, afternoonVolunteerType, specificAreas, shownModal ->
            (specificAreas.size > 1) &&
                    ((morningVolunteerType == FormVolunteerTypeUi.Specific
                            && shownModal == ShownModal.MorningShift)
                            ||
                            (afternoonVolunteerType == FormVolunteerTypeUi.Specific
                                    && shownModal == ShownModal.AfternoonShift)
                    )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )

    private val _selectedMorningSpecificArea = MutableStateFlow<SpecificArea?>(null)
    val selectedMorningSpecificArea: StateFlow<SpecificArea?> =
        _selectedMorningSpecificArea.asStateFlow()

    private val _selectedAfternoonSpecificArea = MutableStateFlow<SpecificArea?>(null)
    val selectedAfternoonSpecificArea: StateFlow<SpecificArea?> =
        _selectedAfternoonSpecificArea.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notAvailableDays =
        date
            .flatMapConcat { date ->
                calendarRepository.getGoogleCalendarEventsByRange(
                    startDate = getStartDate(date),
                    endDate = getLastDate(date)
                ).map { googleCalendarEvents ->
                    googleCalendarEvents.filter { !it.available }.map { it.start.date }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyList(),
            )

    fun onDateChanged(date: LocalDate) {
        _date.update { date }
    }

    fun onShiftSelected(shift: ShiftUi) {
        throttler.throttle {
            _shifts.update { shifts ->
                val mutableShifts = shifts.toMutableList()
                if (shifts.contains(shift)) {
                    mutableShifts.remove(shift)
                    removeShiftInfo(shift)
                } else {
                    mutableShifts.add(shift)
                    when (shift) {
                        ShiftUi.Morning -> openMorningModal()
                        ShiftUi.Afternoon -> openAfternoonModal()
                    }
                }
                mutableShifts.toList()
            }
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

    fun openMorningModal() {
        _shownModal.update { ShownModal.MorningShift }
    }

    fun openAfternoonModal() {
        _shownModal.update { ShownModal.AfternoonShift }
    }

    fun dismissModal() {
        when (_shownModal.value) {
            ShownModal.MorningShift -> {
                val hasSavedInfo = shiftsInfo.value.map { it.shift }.contains(ShiftUi.Morning)
                if (!hasSavedInfo) {
                    val mutableShifts = _shifts.value.toMutableList()
                    mutableShifts.remove(ShiftUi.Morning)
                    _shifts.update { mutableShifts.toList() }
                }
            }
            ShownModal.AfternoonShift -> {
                val hasSavedInfo = shiftsInfo.value.map { it.shift }.contains(ShiftUi.Afternoon)
                if (!hasSavedInfo) {
                    val mutableShifts = _shifts.value.toMutableList()
                    mutableShifts.remove(ShiftUi.Afternoon)
                    _shifts.update { mutableShifts.toList() }
                }
            }
            else -> {}
        }
        _shownModal.update { ShownModal.None }
    }

    fun onMorningVolunteerTypeChanged(type: FormVolunteerTypeUi) {
        _morningVolunteerType.update { type }
    }

    fun onAfternoonVolunteerTypeChanged(type: FormVolunteerTypeUi) {
        _afternoonVolunteerType.update { type }
    }

    fun onMorningStartTimeChanged(time: LocalTime) {
        _morningTimeRange.update { it.copy(start = time) }
    }

    fun onMorningEndTimeChanged(time: LocalTime) {
        _morningTimeRange.update { it.copy(end = time) }
    }

    fun onAfternoonStartTimeChanged(time: LocalTime) {
        _afternoonTimeRange.update { it.copy(start = time) }
    }

    fun onAfternoonEndTimeChanged(time: LocalTime) {
        _afternoonTimeRange.update { it.copy(end = time) }
    }

    fun onMorningSpecificAreaChanged(specificArea: SpecificArea) {
        _selectedMorningSpecificArea.update { specificArea }
    }

    fun onAfternoonSpecificAreaChanged(specificArea: SpecificArea) {
        _selectedAfternoonSpecificArea.update { specificArea }
    }

    fun onConfirmShift() {
        throttler.throttle {
            when (_shownModal.value) {
                ShownModal.MorningShift -> {
                    _shiftsInfo.update { shifts -> addMorningShiftInfo(shifts) }
                }
                ShownModal.AfternoonShift -> {
                    _shiftsInfo.update { shifts -> addAfternoonShiftInfo(shifts) }
                }
                ShownModal.None -> {}
            }
            closeModal()
        }
    }

    private fun closeModal() {
        _shownModal.update { ShownModal.None }
    }

    fun onConfirm() {
        throttler.throttle {
            viewModelScope.launch {
                if (!formInputsAreValid()) {
                    // TODO: Show error
                    return@launch
                }
                if (existVolunteerFromUser()) {
                    _showExistingVolunteerDialogError.update { true }
                    return@launch
                }
                authRepository
                    .getCurrentUser()
                    .onSuccess { user ->
                        volunteerRepository
                            .reserveDay(user.id, buildReservation())
                            .mapCatching {
                                paymentRepository.addPaymentIfNotExist(user.id, date.value!!.toYearMonth())
                            }.onSuccess {
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
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(isFormSavedSuccessfully = false) }
    }

    fun closeExistVolunteerDialog() {
        _showExistingVolunteerDialogError.update { false }
    }

    private fun formInputsAreValid() =
        date.value != null &&
                shifts.value.isNotEmpty() &&
                (morningVolunteerTypeIsValid() || afternoonVolunteerTypeIsValid())

    private fun morningVolunteerTypeIsValid() =
       (morningVolunteerType.value == FormVolunteerTypeUi.Specific &&
                selectedMorningSpecificArea.value == null) ||
                morningVolunteerType.value == FormVolunteerTypeUi.General

    private fun afternoonVolunteerTypeIsValid() =
        (afternoonVolunteerType.value == FormVolunteerTypeUi.Specific &&
                selectedAfternoonSpecificArea.value == null) ||
                afternoonVolunteerType.value == FormVolunteerTypeUi.General

    private fun buildReservation() =
        Volunteer(
            id = VolunteerId.Empty,
            userId = UserId.Empty,
            date = date.value!!,
            shifts = shifts.value.toDomainModel(),
            meals = additionalOptions.value.getMeals(),
            sleep = additionalOptions.value.contains(AdditionalOption.Sleep),
        )

    private fun navigateBack() {
        _uiState.update { it.copy(isFormSavedSuccessfully = true) }
    }

    private fun addMorningShiftInfo(shiftsInfo: List<ShiftInfoSummary>): List<ShiftInfoSummary> {
        val newShift = ShiftInfoSummary(
            shift = ShiftUi.Morning,
            timeRange = morningTimeRange.value,
            type = morningVolunteerType.value,
            specificArea = selectedMorningSpecificArea.value,
        )
        if (shiftsInfo.isEmpty()) {
            return listOf(newShift)
        }
        val mutableInfo = shiftsInfo.toMutableList()
        if (shiftsInfo.any { it.shift == ShiftUi.Morning }) {
            mutableInfo.set(index = 0, element = newShift)
        } else {
            mutableInfo.add(index = 0, element = newShift)
        }
        return mutableInfo.toList()
    }

    private fun addAfternoonShiftInfo(shiftsInfo: List<ShiftInfoSummary>): List<ShiftInfoSummary> {
        val newShift = ShiftInfoSummary(
            shift = ShiftUi.Afternoon,
            timeRange = afternoonTimeRange.value,
            type = afternoonVolunteerType.value,
            specificArea = selectedAfternoonSpecificArea.value,
        )
        if (shiftsInfo.isEmpty()) {
            return listOf(newShift)
        }
        val mutableInfo = shiftsInfo.toMutableList()
        if (shiftsInfo.any { it.shift == ShiftUi.Afternoon }) {
            val index = shiftsInfo.indexOfFirst { it.shift == ShiftUi.Afternoon }
            mutableInfo.set(index = index, element = newShift)
        } else {
            mutableInfo.add(newShift)
        }
        return mutableInfo.toList()
    }

    private fun removeShiftInfo(from: ShiftUi) {
        _shiftsInfo.update { shiftsInfo ->
            val mutableInfo = shiftsInfo.toMutableList()
            mutableInfo.removeAll { it.shift == from }
            mutableInfo.toList()
        }
    }

    private fun List<AdditionalOption>.getMeals() =
        filter { it != AdditionalOption.Sleep }.map { it.toMeal() }

    private fun AdditionalOption.toMeal() =
        when (this) {
            AdditionalOption.Lunch -> Meal.Lunch
            AdditionalOption.Dinner -> Meal.Dinner
            else -> Meal.Unknown
        }

    private fun List<ShiftUi>.toDomainModel() =
        when {
            containsAll(ShiftUi.entries.toList()) -> this.map {
                when (it) {
                    ShiftUi.Morning -> Shift.Morning(
                        type = morningVolunteerType.value
                            .toDomainModel(selectedMorningSpecificArea.value),
                        timeRange = morningTimeRange.value,
                    )
                    ShiftUi.Afternoon -> Shift.Afternoon(
                        type = afternoonVolunteerType.value
                            .toDomainModel(selectedAfternoonSpecificArea.value),
                        timeRange = afternoonTimeRange.value,
                    )
                }
            }
            else -> {
                when (this.first()) {
                    ShiftUi.Morning -> listOf(
                        Shift.Morning(
                            type = morningVolunteerType.value
                                .toDomainModel(selectedMorningSpecificArea.value),
                            timeRange = morningTimeRange.value,
                        )
                    )
                    ShiftUi.Afternoon -> listOf(
                        Shift.Afternoon(
                            type = afternoonVolunteerType.value
                                .toDomainModel(selectedAfternoonSpecificArea.value),
                            timeRange = afternoonTimeRange.value,
                        )
                    )
                }
            }
        }

    private fun FormVolunteerTypeUi.toDomainModel(specificArea: SpecificArea? = null) =
        when (this) {
            FormVolunteerTypeUi.General -> VolunteerType.General
            FormVolunteerTypeUi.Specific -> VolunteerType.Specific(specificArea!!)
        }

    private suspend fun existVolunteerFromUser(): Boolean {
        var result = false
        // TODO: Handle this error with a message
        val user = authRepository.getCurrentUser().getOrNull() ?: return true
        volunteerRepository
            .getVolunteersByUser(user.id)
            .onSuccess { volunteers ->
                result = volunteers.any { it.date == date.value }
            }.onFailure {
                result = true
            }
        return result
    }

    private fun getStartDate(date: LocalDate?) =
        date?.getFirstDayOfMonth() ?: Clock.System.now().toDate().getFirstDayOfMonth()

    private fun getLastDate(date: LocalDate?) =
        date?.getLastDayOfMonth() ?: Clock.System.now().toDate().getLastDayOfMonth()
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

enum class FormVolunteerTypeUi(
    val text: StringResource,
    val icon: DrawableResource,
) {
    General(
        text = Res.string.general,
        icon = Res.drawable.ic_group,
    ),
    Specific(
        text = Res.string.specific,
        icon = Res.drawable.ic_target,
    ),
}

enum class ShownModal {
    MorningShift,
    AfternoonShift,
    None,
}

data class ShiftInfoSummary(
    val shift: ShiftUi,
    val timeRange: TimeRange,
    val type: FormVolunteerTypeUi,
    val specificArea: SpecificArea? = null,
)

private fun LocalDate.toYearMonth() =
    YearMonth(
        year = this.year,
        month = this.month,
    )

private const val LOG_TAG = "ReservationFormViewModel"
