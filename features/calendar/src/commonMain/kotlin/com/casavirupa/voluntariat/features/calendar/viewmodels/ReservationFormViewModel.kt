package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.features.calendar.navigation.ReservationFormNavKey
import com.casavirupa.voluntariat.shared.core.utils.Throttler
import com.casavirupa.voluntariat.shared.domain.AreaConfigRepository
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.configuration.AreaConfig
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import voluntariatcv.features.calendar.generated.resources.Res
import voluntariatcv.features.calendar.generated.resources.afternoon
import voluntariatcv.features.calendar.generated.resources.breakfast
import voluntariatcv.features.calendar.generated.resources.breakfast_next_day
import voluntariatcv.features.calendar.generated.resources.dinner
import voluntariatcv.features.calendar.generated.resources.general
import voluntariatcv.features.calendar.generated.resources.ic_afternoon
import voluntariatcv.features.calendar.generated.resources.ic_breakfast
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

class ReservationFormViewModel(
    navKey: ReservationFormNavKey,
    private val authRepository: AuthRepository,
    private val volunteerRepository: VolunteerRepository,
    private val calendarRepository: CalendarRepository,
    private val areaConfigRepository: AreaConfigRepository,
) : ViewModel() {
    private val throttler = Throttler()

    // Own throttler so a tap on CONFIRMAR right after a shift modal's confirm isn't swallowed
    private val confirmThrottler = Throttler()

    // Dashboard-published list of areas whose volunteering may be done online.
    val areaConfig: StateFlow<AreaConfig> =
        areaConfigRepository
            .getAreaConfig()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = AreaConfig.Empty,
            )

    // True once the user accepted to volunteer on a day with no on-site volunteering
    // ("NO VOLUNTARIAT" in the Google Calendar): every shift is online, only online-capable
    // areas can be chosen and there are no meals or nights to book.
    private val _forcedOnline = MutableStateFlow(false)
    val forcedOnline: StateFlow<Boolean> = _forcedOnline.asStateFlow()

    private val _morningOnline = MutableStateFlow(false)
    val morningOnline: StateFlow<Boolean> = _morningOnline.asStateFlow()

    private val _afternoonOnline = MutableStateFlow(false)
    val afternoonOnline: StateFlow<Boolean> = _afternoonOnline.asStateFlow()

    private val _uiState = MutableStateFlow(ReservationFormUiState())
    val uiState: StateFlow<ReservationFormUiState> = _uiState.asStateFlow()

    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()

    private val _shifts = MutableStateFlow<List<ShiftUi>>(emptyList())
    val shifts: StateFlow<List<ShiftUi>> = _shifts.asStateFlow()

    private val _additionalOptionsSelected = MutableStateFlow<List<AdditionalOption>>(emptyList())
    val additionalOptionsSelected: StateFlow<List<AdditionalOption>> =
        _additionalOptionsSelected.asStateFlow()

    // Sleeping over is reserved to members; the next-day breakfast only makes sense
    // together with an overnight stay, so it appears once "Sleep" is selected.
    val additionalOptions: StateFlow<List<AdditionalOption>> =
        combine(
            authRepository.getCurrentUserFlow(),
            _additionalOptionsSelected,
            _forcedOnline,
        ) { user, selected, forcedOnline ->
            if (!user.paysForServices || forcedOnline) return@combine emptyList()
            AdditionalOption.entries.filter { option ->
                when (option) {
                    AdditionalOption.Sleep -> user.isMember
                    AdditionalOption.BreakfastNextDay ->
                        user.isMember && AdditionalOption.Sleep in selected
                    else -> true
                }
            }
        }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyList(),
            )

    // Non-member habituals can't book a night from the app: they have to arrange it with
    // the guesthouse ("hostatgeria") directly, so the form tells them so.
    val showSleepNotice: StateFlow<Boolean> =
        combine(
            authRepository.getCurrentUserFlow(),
            _forcedOnline,
        ) { user, forcedOnline ->
            user.paysForServices && user.isHabitual && !user.isMember && !forcedOnline
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )

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

    // The areas the user may pick for a specific shift: all their areas normally, only the
    // online-capable ones on a day without on-site volunteering.
    val specificAreas: StateFlow<List<SpecificArea>> =
        combine(
            authRepository.getCurrentUserFlow(),
            areaConfig,
            _forcedOnline,
        ) { user, config, forcedOnline ->
            if (forcedOnline) config.onlineAreasOf(user) else user.specificAreas
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    // General (on-site) volunteering isn't offered on a forced-online day.
    val volunteerTypeOptions: StateFlow<List<FormVolunteerTypeUi>> =
        _forcedOnline
            .map { forcedOnline ->
                if (forcedOnline) listOf(FormVolunteerTypeUi.Specific) else FormVolunteerTypeUi.entries
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = FormVolunteerTypeUi.entries,
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

    // Type and area of the shift whose modal is open, or null when none is.
    private val openShiftSelection: StateFlow<Pair<FormVolunteerTypeUi, SpecificArea?>?> =
        combine(
            shownModal,
            morningVolunteerType,
            afternoonVolunteerType,
            selectedMorningSpecificArea,
            selectedAfternoonSpecificArea,
        ) { modal, morningType, afternoonType, morningArea, afternoonArea ->
            when (modal) {
                ShownModal.MorningShift -> morningType to morningArea
                ShownModal.AfternoonShift -> afternoonType to afternoonArea
                ShownModal.None -> null
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // The online switch appears only for a specific shift whose area allows remote work.
    // With a single selectable area there is no area picker, so that area is the one used.
    val showOnlineToggle: StateFlow<Boolean> =
        combine(
            openShiftSelection,
            specificAreas,
            areaConfig,
        ) { selection, areas, config ->
            selection != null &&
                selection.first == FormVolunteerTypeUi.Specific &&
                config.allowsOnline(selection.second ?: areas.singleOrNull())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )

    private val _remoteDialog = MutableStateFlow(RemoteDialog.None)
    val remoteDialog: StateFlow<RemoteDialog> = _remoteDialog.asStateFlow()

    private var temporalDate: LocalDate? = null

    init {
        // Opened from a day's detail: go through the same path as a manual pick so the
        // «NO VOLUNTARIAT» check (and its online dialog) also applies to the pre-filled date.
        navKey.initialDate?.let(::onDateChanged)
    }

    fun onDateChanged(date: LocalDate) {
        viewModelScope.launch {
            if (isVolunteeringAvailableOn(date)) {
                _forcedOnline.update { false }
                applySelectedDate(date)
            } else {
                temporalDate = date
                _remoteDialog.update {
                    if (canWorkOnlineOnUnavailableDay()) {
                        RemoteDialog.CanGoOnline
                    } else {
                        RemoteDialog.NoOnlineAreas
                    }
                }
            }
        }
    }

    private suspend fun canWorkOnlineOnUnavailableDay(): Boolean {
        val user = authRepository.getCurrentUser().getOrNull() ?: return false
        val config = runCatching { areaConfigRepository.getAreaConfig().first() }
            .getOrDefault(AreaConfig.Empty)
        return config.onlineAreasOf(user).isNotEmpty()
    }

    /**
     * A day is not available for on-site volunteering when any Google Calendar event happening
     * on it (all-day or multi-day included) is flagged as not available ("NO VOLUNTARIAT").
     * If the events can't be read, the day is treated as available so the user isn't blocked.
     */
    private suspend fun isVolunteeringAvailableOn(date: LocalDate): Boolean =
        runCatching {
            calendarRepository
                .getGoogleCalendarEventsByDate(date)
                .first()
                .all { it.available }
        }.getOrElse { error ->
            Logger.e(error, LOG_TAG) { "Error checking availability on $date" }
            true
        }

    /**
     * The user accepted to volunteer online on a day without on-site volunteering: every
     * shift becomes online and specific, and anything already configured for the previous
     * date (shifts, meals, nights) is dropped because it may not be allowed any more.
     */
    fun workOnRemoteOnDate() {
        temporalDate?.let { date ->
            _forcedOnline.update { true }
            _shifts.update { emptyList() }
            _shiftsInfo.update { emptyList() }
            _additionalOptionsSelected.update { emptyList() }
            _morningVolunteerType.update { FormVolunteerTypeUi.Specific }
            _afternoonVolunteerType.update { FormVolunteerTypeUi.Specific }
            _morningOnline.update { true }
            _afternoonOnline.update { true }
            _selectedMorningSpecificArea.update { null }
            _selectedAfternoonSpecificArea.update { null }
            applySelectedDate(date)
        }
        closeRemoteDialog()
    }

    private fun applySelectedDate(date: LocalDate) {
        _date.update { date }
        applyDefaultTimeRanges(date)
    }

    private fun applyDefaultTimeRanges(date: LocalDate) {
        val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
        if (_shiftsInfo.value.none { it.shift == ShiftUi.Morning }) {
            _morningTimeRange.update { TimeRange.defaultMorning(isSunday) }
        }
        if (_shiftsInfo.value.none { it.shift == ShiftUi.Afternoon }) {
            _afternoonTimeRange.update { TimeRange.defaultAfternoon(isSunday) }
        }
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
        _additionalOptionsSelected.update {
            val mutableOptions = it.toMutableList()
            if (mutableOptions.contains(option)) {
                mutableOptions.remove(option)
                // No overnight stay → no breakfast the morning after
                if (option == AdditionalOption.Sleep) {
                    mutableOptions.remove(AdditionalOption.BreakfastNextDay)
                }
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
        // General volunteering is always on-site
        if (type == FormVolunteerTypeUi.General) _morningOnline.update { false }
    }

    fun onAfternoonVolunteerTypeChanged(type: FormVolunteerTypeUi) {
        _afternoonVolunteerType.update { type }
        if (type == FormVolunteerTypeUi.General) _afternoonOnline.update { false }
    }

    fun onMorningOnlineChanged(online: Boolean) {
        if (!_forcedOnline.value) _morningOnline.update { online }
    }

    fun onAfternoonOnlineChanged(online: Boolean) {
        if (!_forcedOnline.value) _afternoonOnline.update { online }
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
        // Switching to an area that can't be worked remotely drops the online choice
        if (!areaConfig.value.allowsOnline(specificArea)) _morningOnline.update { false }
    }

    fun onAfternoonSpecificAreaChanged(specificArea: SpecificArea) {
        _selectedAfternoonSpecificArea.update { specificArea }
        if (!areaConfig.value.allowsOnline(specificArea)) _afternoonOnline.update { false }
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
        confirmThrottler.throttle {
            viewModelScope.launch {
                formError()?.let { error ->
                    showError(error)
                    return@launch
                }
                val user = authRepository.getCurrentUser().getOrElse { error ->
                    Logger.e(error, LOG_TAG) { "Error getting current user when reserving a day" }
                    showError(ReservationFormError.SaveFailed)
                    return@launch
                }
                val alreadyBooked = existVolunteerFromUser(user.id).getOrElse { error ->
                    Logger.e(error, LOG_TAG) { "Error reading the user's volunteers" }
                    showError(ReservationFormError.SaveFailed)
                    return@launch
                }
                if (alreadyBooked) {
                    _showExistingVolunteerDialogError.update { true }
                    return@launch
                }
                volunteerRepository
                    .reserveDay(user.id, buildReservation())
                    .onSuccess {
                        navigateBack()
                    }.onFailure { error ->
                        Logger.e(error, LOG_TAG) { "Error reserving a day" }
                        showError(ReservationFormError.SaveFailed)
                    }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun showError(error: ReservationFormError) {
        _uiState.update { it.copy(error = error) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(isFormSavedSuccessfully = false) }
    }

    fun closeExistVolunteerDialog() {
        _showExistingVolunteerDialogError.update { false }
    }

    fun closeRemoteDialog() {
        _remoteDialog.update { RemoteDialog.None }
        temporalDate = null
    }

    // First reason the form can't be saved yet, or null when it's ready
    private fun formError(): ReservationFormError? =
        when {
            date.value == null -> ReservationFormError.MissingDate
            // No shift is fine when the volunteer only comes to eat or sleep (issue #108)
            shifts.value.isEmpty() && _additionalOptionsSelected.value.isEmpty() ->
                if (additionalOptions.value.isEmpty()) {
                    ReservationFormError.MissingShift
                } else {
                    ReservationFormError.MissingShiftOrOption
                }
            !shifts.value.all(::shiftAreaIsValid) -> ReservationFormError.MissingSpecificArea
            !onlineConstraintsAreValid() -> ReservationFormError.OnlineAreaRequired
            else -> null
        }

    // On a forced-online day every confirmed shift must be specific, in an online area
    private fun onlineConstraintsAreValid(): Boolean {
        if (!_forcedOnline.value) return true
        val config = areaConfig.value
        val fallbackArea = fallbackSpecificArea()
        val confirmed = shiftsInfo.value
        return confirmed.isNotEmpty() && confirmed.all { info ->
            info.type == FormVolunteerTypeUi.Specific &&
                config.allowsOnline(info.specificArea ?: fallbackArea)
        }
    }

    // A general shift needs nothing more; a specific one needs an area, either picked or
    // the only one the user can choose (there's no picker then).
    private fun shiftAreaIsValid(shift: ShiftUi): Boolean =
        when (shift) {
            ShiftUi.Morning -> morningVolunteerType.value == FormVolunteerTypeUi.General ||
                (selectedMorningSpecificArea.value ?: fallbackSpecificArea()) != null
            ShiftUi.Afternoon -> afternoonVolunteerType.value == FormVolunteerTypeUi.General ||
                (selectedAfternoonSpecificArea.value ?: fallbackSpecificArea()) != null
        }

    // Area used by a specific shift when there was no picker: the single selectable area.
    // On a forced-online day that's the single ONLINE area, not the user's first area.
    private fun fallbackSpecificArea(): SpecificArea? = specificAreas.value.singleOrNull()

    private fun buildReservation() =
        Volunteer(
            id = VolunteerId.Empty,
            userId = UserId.Empty,
            date = date.value!!,
            shifts = shifts.value.toDomainModel(fallbackSpecificArea()),
            meals = additionalOptionsSelected.value.getMeals(),
            sleep = additionalOptionsSelected.value.contains(AdditionalOption.Sleep),
        )

    private fun navigateBack() {
        _uiState.update { it.copy(isFormSavedSuccessfully = true) }
    }

    // A shift is online only if the user asked for it AND it is a specific shift in an area
    // that allows remote work; the toggle is hidden otherwise, but the state is re-checked
    // here so a stale value can never be persisted.
    private fun morningIsOnline(fallbackArea: SpecificArea? = fallbackSpecificArea()) =
        _morningOnline.value &&
            morningVolunteerType.value == FormVolunteerTypeUi.Specific &&
            areaConfig.value.allowsOnline(selectedMorningSpecificArea.value ?: fallbackArea)

    private fun afternoonIsOnline(fallbackArea: SpecificArea? = fallbackSpecificArea()) =
        _afternoonOnline.value &&
            afternoonVolunteerType.value == FormVolunteerTypeUi.Specific &&
            areaConfig.value.allowsOnline(selectedAfternoonSpecificArea.value ?: fallbackArea)

    private fun addMorningShiftInfo(shiftsInfo: List<ShiftInfoSummary>): List<ShiftInfoSummary> {
        val newShift = ShiftInfoSummary(
            shift = ShiftUi.Morning,
            timeRange = morningTimeRange.value,
            type = morningVolunteerType.value,
            specificArea = selectedMorningSpecificArea.value,
            online = morningIsOnline(),
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
            online = afternoonIsOnline(),
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
            AdditionalOption.Breakfast -> Meal.Breakfast
            AdditionalOption.BreakfastNextDay -> Meal.BreakfastNextDay
            AdditionalOption.Lunch -> Meal.Lunch
            AdditionalOption.Dinner -> Meal.Dinner
            else -> Meal.Unknown
        }

    private fun List<ShiftUi>.toDomainModel(userSpecificArea: SpecificArea?) =
        when {
            isEmpty() -> emptyList()
            containsAll(ShiftUi.entries.toList()) -> this.map {
                when (it) {
                    ShiftUi.Morning -> morningShift(userSpecificArea)
                    ShiftUi.Afternoon -> afternoonShift(userSpecificArea)
                }
            }
            else -> {
                when (this.first()) {
                    ShiftUi.Morning -> listOf(morningShift(userSpecificArea))
                    ShiftUi.Afternoon -> listOf(afternoonShift(userSpecificArea))
                }
            }
        }

    private fun morningShift(fallbackArea: SpecificArea?): Shift.Morning {
        val specificArea = selectedMorningSpecificArea.value ?: fallbackArea
        return Shift.Morning(
            type = morningVolunteerType.value.toDomainModel(specificArea),
            timeRange = morningTimeRange.value,
            online = morningIsOnline(fallbackArea),
        )
    }

    private fun afternoonShift(fallbackArea: SpecificArea?): Shift.Afternoon {
        val specificArea = selectedAfternoonSpecificArea.value ?: fallbackArea
        return Shift.Afternoon(
            type = afternoonVolunteerType.value.toDomainModel(specificArea),
            timeRange = afternoonTimeRange.value,
            online = afternoonIsOnline(fallbackArea),
        )
    }

    private fun FormVolunteerTypeUi.toDomainModel(specificArea: SpecificArea? = null) =
        when (this) {
            FormVolunteerTypeUi.General -> VolunteerType.General
            FormVolunteerTypeUi.Specific -> VolunteerType.Specific(specificArea!!)
        }

    private suspend fun existVolunteerFromUser(userId: UserId): Result<Boolean> =
        volunteerRepository
            .getVolunteersByUser(userId)
            .map { volunteers -> volunteers.any { it.date == date.value } }
}

data class ReservationFormUiState(
    val isFormSavedSuccessfully: Boolean = false,
    val error: ReservationFormError? = null,
)

/** Why CONFIRMAR couldn't save the booking; shown to the user in a dialog. */
enum class ReservationFormError {
    MissingDate,
    MissingShift,
    // Meals or a bed can be booked without a shift, so any of them would do
    MissingShiftOrOption,
    MissingSpecificArea,
    OnlineAreaRequired,
    SaveFailed,
}

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
    Breakfast(
        text = Res.string.breakfast,
        icon = Res.drawable.ic_breakfast,
    ),
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
    BreakfastNextDay(
        text = Res.string.breakfast_next_day,
        icon = Res.drawable.ic_breakfast,
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

/** Dialog shown when the chosen day has no on-site volunteering ("NO VOLUNTARIAT"). */
enum class RemoteDialog {
    None,
    /** The user has at least one online-capable area: they may continue, online only. */
    CanGoOnline,
    /** None of the user's areas allows remote work: the day can't be booked. */
    NoOnlineAreas,
}

data class ShiftInfoSummary(
    val shift: ShiftUi,
    val timeRange: TimeRange,
    val type: FormVolunteerTypeUi,
    val specificArea: SpecificArea? = null,
    val online: Boolean = false,
)

private const val LOG_TAG = "ReservationFormViewModel"
