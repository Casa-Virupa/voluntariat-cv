package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.features.calendar.navigation.ReservationFormNavKey
import com.casavirupa.voluntariat.shared.core.utils.Throttler
import com.casavirupa.voluntariat.shared.domain.AreaConfigRepository
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.ScheduleConfigRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.DayCoverage
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.configuration.AreaConfig
import com.casavirupa.voluntariat.shared.model.configuration.ScheduleConfig
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.TimeRange
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.calendar.bookableServices
import com.casavirupa.voluntariat.shared.model.calendar.unavailabilityOn
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
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
    scheduleConfigRepository: ScheduleConfigRepository,
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

    // When on-site volunteering ends on Sundays (`configuration/schedule`). Started eagerly so
    // it's usually loaded by the time a date is picked and the default times are applied.
    private val scheduleConfig: StateFlow<ScheduleConfig> =
        scheduleConfigRepository
            .getScheduleConfig()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = ScheduleConfig.Default,
            )

    // True once the user accepted to volunteer on a day with no on-site volunteering
    // ("NO VOLUNTARIAT" in the Google Calendar): every shift is online, only online-capable
    // areas can be chosen and there are no meals or nights to book.
    private val _forcedOnline = MutableStateFlow(false)
    val forcedOnline: StateFlow<Boolean> = _forcedOnline.asStateFlow()

    private val _uiState = MutableStateFlow(ReservationFormUiState())
    val uiState: StateFlow<ReservationFormUiState> = _uiState.asStateFlow()

    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()

    private val _additionalOptionsSelected = MutableStateFlow<List<AdditionalOption>>(emptyList())
    val additionalOptionsSelected: StateFlow<List<AdditionalOption>> =
        _additionalOptionsSelected.asStateFlow()

    // The confirmed shifts, in start order. A morning or afternoon may hold several of them as
    // long as no two overlap (issue #113).
    private val _shiftsInfo = MutableStateFlow<List<ShiftInfoSummary>>(emptyList())
    val shiftsInfo: StateFlow<List<ShiftInfoSummary>> = _shiftsInfo.asStateFlow()

    // Local key of the next confirmed shift, so each summary row can be edited or removed
    private var nextShiftId = 0L

    // The user's own bookings on the days around the chosen date: an on-site morning shift the
    // next day lets a non-mitra book the night before, and a breakfast either of them already
    // covers isn't offered again. A read error only hides / keeps those options.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val neighbourBookings: Flow<NeighbourBookings> =
        combine(
            authRepository.getCurrentUserFlow()
                .flatMapLatest { user -> volunteerRepository.getVolunteersByUserFlow(user.id) }
                .catch { error ->
                    Logger.e(error, LOG_TAG) { "Error reading the user's volunteers" }
                    emit(emptyList())
                },
            _date,
        ) { volunteers, date ->
            if (date == null) return@combine NeighbourBookings()
            NeighbourBookings(
                previousDay = volunteers.firstOrNull { it.date == date.minus(1, DateTimeUnit.DAY) },
                nextDay = volunteers.firstOrNull { it.date == date.plus(1, DateTimeUnit.DAY) },
            )
        }

    // Mitras may book meals and nights freely; everyone else only what the day's confirmed
    // on-site shifts justify (see BookableServices). Sleeping over is reserved to members, and
    // the next-day breakfast only makes sense with an overnight stay, so it appears once
    // "Sleep" is selected.
    val additionalOptions: StateFlow<List<AdditionalOption>> =
        combine(
            authRepository.getCurrentUserFlow(),
            _additionalOptionsSelected,
            _forcedOnline,
            _shiftsInfo,
            neighbourBookings,
        ) { user, selected, forcedOnline, shiftsInfo, neighbours ->
            if (!user.paysForServices || forcedOnline) return@combine emptyList()
            val onSite = shiftsInfo.filterNot { it.online }.map { it.shift }
            val bookable = user.bookableServices(
                morning = ShiftUi.Morning in onSite,
                afternoon = ShiftUi.Afternoon in onSite,
                nextDay = neighbours.nextDay,
                previousDay = neighbours.previousDay,
            )
            AdditionalOption.entries.filter { option ->
                when (option) {
                    AdditionalOption.Sleep -> user.isMember && bookable.sleep
                    AdditionalOption.BreakfastNextDay ->
                        user.isMember && AdditionalOption.Sleep in selected &&
                            option.toMeal() in bookable.meals
                    else -> option.toMeal() in bookable.meals
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

    // The shift being added or edited in the modal, or null when it's closed
    private val _editor = MutableStateFlow<ShiftEditor?>(null)
    val editor: StateFlow<ShiftEditor?> = _editor.asStateFlow()

    // The Sunday end time to warn about while the open shift modal's times go past it (issue
    // #105), or null. Only a warning: the shift can still be confirmed.
    val sundayEndWarning: StateFlow<LocalTime?> =
        combine(_editor, _date, scheduleConfig) { editor, date, config ->
            editor ?: return@combine null
            config.sundayEndTime.takeIf {
                config.isLateOnSunday(date, editor.timeRange, editor.online)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // Confirmed shifts (by id) that end after the Sunday limit, flagged in the summary with it.
    val lateSundayShifts: StateFlow<Map<Long, LocalTime>> =
        combine(_shiftsInfo, _date, scheduleConfig) { shiftsInfo, date, config ->
            shiftsInfo
                .filter { config.isLateOnSunday(date, it.timeRange, it.online) }
                .associate { it.id to config.sundayEndTime }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyMap(),
        )

    // The confirmed shift the open modal's times overlap, if any: the only thing that stops a
    // shift from being added (issue #113).
    val overlappingShift: StateFlow<ShiftInfoSummary?> =
        combine(_editor, _shiftsInfo) { editor, shiftsInfo ->
            editor?.let { shiftsInfo.overlapping(it) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

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

    // General (on-site) volunteering isn't offered on a forced-online day, and specific
    // volunteering isn't offered to someone with no specific area to pick.
    val volunteerTypeOptions: StateFlow<List<FormVolunteerTypeUi>> =
        combine(_forcedOnline, specificAreas) { forcedOnline, areas ->
            when {
                forcedOnline -> listOf(FormVolunteerTypeUi.Specific)
                areas.isEmpty() -> listOf(FormVolunteerTypeUi.General)
                else -> FormVolunteerTypeUi.entries
            }
        }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = FormVolunteerTypeUi.entries,
            )

    val showSpecificAreaSelector: StateFlow<Boolean> =
        combine(_editor, specificAreas) { editor, specificAreas ->
            specificAreas.size > 1 && editor?.type == FormVolunteerTypeUi.Specific
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false,
        )

    // The online switch appears only for a specific shift whose area allows remote work.
    // With a single selectable area there is no area picker, so that area is the one used.
    val showOnlineToggle: StateFlow<Boolean> =
        combine(
            _editor,
            specificAreas,
            areaConfig,
        ) { editor, areas, config ->
            editor != null &&
                editor.type == FormVolunteerTypeUi.Specific &&
                config.allowsOnline(editor.specificArea ?: areas.singleOrNull())
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
        // Removing a shift, making it online or changing the date can withdraw options that
        // were already picked: drop them so they're never booked (converges, since dropping
        // Sleep also withdraws the next-day breakfast).
        viewModelScope.launch {
            additionalOptions.collect { offered ->
                _additionalOptionsSelected.update { selected -> selected.filter { it in offered } }
            }
        }
    }

    fun onDateChanged(date: LocalDate) {
        viewModelScope.launch {
            if (isVolunteeringAvailableOn(date)) {
                _forcedOnline.update { false }
                _date.update { date }
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
     * A day is not available for on-site volunteering when its «NO VOLUNTARIAT» events
     * (all-day or multi-day included) cover the whole of it; a timed one covering only the
     * morning or the afternoon leaves the day bookable.
     * If the events can't be read, the day is treated as available so the user isn't blocked.
     */
    private suspend fun isVolunteeringAvailableOn(date: LocalDate): Boolean =
        runCatching {
            calendarRepository
                .getGoogleCalendarEventsByDate(date)
                .first()
                .unavailabilityOn(date) != DayCoverage.WholeDay
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
            _shiftsInfo.update { emptyList() }
            _additionalOptionsSelected.update { emptyList() }
            _editor.update { null }
            _date.update { date }
        }
        closeRemoteDialog()
    }

    // Tapping «Matí» or «Tarda» always opens a new shift of that half-day: a volunteer may do
    // several in one morning or afternoon, e.g. in two of their areas (issue #113).
    fun onAddShift(shift: ShiftUi) {
        throttler.throttle {
            val forcedOnline = _forcedOnline.value
            _editor.update {
                ShiftEditor(
                    editingId = null,
                    shift = shift,
                    type = if (forcedOnline) FormVolunteerTypeUi.Specific else FormVolunteerTypeUi.General,
                    timeRange = defaultTimeRange(shift),
                    online = forcedOnline,
                )
            }
        }
    }

    fun onEditShift(id: Long) {
        val info = _shiftsInfo.value.find { it.id == id } ?: return
        _editor.update {
            ShiftEditor(
                editingId = info.id,
                shift = info.shift,
                type = info.type,
                timeRange = info.timeRange,
                specificArea = info.specificArea,
                online = info.online,
            )
        }
    }

    fun onRemoveShift(id: Long) {
        _shiftsInfo.update { shiftsInfo -> shiftsInfo.filterNot { it.id == id } }
    }

    // The house's usual hours for the half-day; on Sundays the afternoon ends when the house
    // says so, never past the warning. A further shift in the same half-day starts where the
    // last one ends, so it doesn't open overlapping.
    private fun defaultTimeRange(shift: ShiftUi): TimeRange {
        val isSunday = _date.value?.dayOfWeek == DayOfWeek.SUNDAY
        val default = when (shift) {
            ShiftUi.Morning -> TimeRange.defaultMorning(isSunday)
            ShiftUi.Afternoon -> TimeRange.defaultAfternoon(isSunday).let { afternoon ->
                if (isSunday) afternoon.copy(end = scheduleConfig.value.sundayEndTime) else afternoon
            }
        }
        val lastEnd = _shiftsInfo.value
            .filter { it.shift == shift }
            .maxOfOrNull { it.timeRange.end }
            ?: return default
        return if (lastEnd < default.end) {
            default.copy(start = maxOf(lastEnd, default.start))
        } else {
            default
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

    fun dismissModal() {
        _editor.update { null }
    }

    fun onVolunteerTypeChanged(type: FormVolunteerTypeUi) {
        // General volunteering is always on-site
        _editor.update { editor ->
            editor?.copy(type = type, online = editor.online && type == FormVolunteerTypeUi.Specific)
        }
    }

    fun onOnlineChanged(online: Boolean) {
        if (!_forcedOnline.value) _editor.update { it?.copy(online = online) }
    }

    fun onStartTimeChanged(time: LocalTime) {
        _editor.update { it?.copy(timeRange = it.timeRange.copy(start = time)) }
    }

    fun onEndTimeChanged(time: LocalTime) {
        _editor.update { it?.copy(timeRange = it.timeRange.copy(end = time)) }
    }

    fun onSpecificAreaChanged(specificArea: SpecificArea) {
        // Switching to an area that can't be worked remotely drops the online choice
        _editor.update { editor ->
            editor?.copy(
                specificArea = specificArea,
                online = editor.online && areaConfig.value.allowsOnline(specificArea),
            )
        }
    }

    // An overlapping shift is refused (the modal shows why); otherwise it's added, or replaces
    // the one being edited.
    fun onConfirmShift() {
        throttler.throttle {
            val editor = _editor.value ?: return@throttle
            if (_shiftsInfo.value.overlapping(editor) != null) return@throttle
            val info = ShiftInfoSummary(
                id = editor.editingId ?: nextShiftId++,
                shift = editor.shift,
                timeRange = editor.timeRange,
                type = editor.type,
                specificArea = editor.specificArea,
                online = isOnline(editor.online, editor.type, editor.specificArea),
            )
            _shiftsInfo.update { shiftsInfo ->
                (shiftsInfo.filterNot { it.id == info.id } + info).sortedBy { it.timeRange.start }
            }
            closeModal()
        }
    }

    private fun closeModal() {
        _editor.update { null }
    }

    fun onConfirm() {
        // The duplicate-day check and the write are separate network calls: a second tap
        // while they run (slow connection) would book the same day twice.
        if (_uiState.value.isSaving) return
        confirmThrottler.throttle {
            viewModelScope.launch {
                formError()?.let { error ->
                    showError(error)
                    return@launch
                }
                _uiState.update { it.copy(isSaving = true) }
                if (!saveReservation()) _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    // True once the booking is written; on any other outcome the user has been told why.
    private suspend fun saveReservation(): Boolean {
        val user = authRepository.getCurrentUser().getOrElse { error ->
            Logger.e(error, LOG_TAG) { "Error getting current user when reserving a day" }
            showError(ReservationFormError.SaveFailed)
            return false
        }
        val alreadyBooked = existVolunteerFromUser(user.id).getOrElse { error ->
            Logger.e(error, LOG_TAG) { "Error reading the user's volunteers" }
            showError(ReservationFormError.SaveFailed)
            return false
        }
        if (alreadyBooked) {
            _showExistingVolunteerDialogError.update { true }
            return false
        }
        return volunteerRepository
            .reserveDay(user.id, buildReservation())
            .onSuccess {
                navigateBack()
            }.onFailure { error ->
                Logger.e(error, LOG_TAG) { "Error reserving a day" }
                showError(ReservationFormError.SaveFailed)
            }.isSuccess
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
            // No shift is fine when a mitra only comes to eat or sleep (issue #108), or for
            // the night before a booked on-site morning shift
            shiftsInfo.value.isEmpty() && _additionalOptionsSelected.value.isEmpty() ->
                if (additionalOptions.value.isEmpty()) {
                    ReservationFormError.MissingShift
                } else {
                    ReservationFormError.MissingShiftOrOption
                }
            !shiftsInfo.value.all(::shiftAreaIsValid) -> ReservationFormError.MissingSpecificArea
            // Already refused shift by shift; re-checked so an overlap never gets booked
            shiftsInfo.value.any { info -> shiftsInfo.value.overlapping(info) != null } ->
                ReservationFormError.OverlappingShifts
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
    private fun shiftAreaIsValid(info: ShiftInfoSummary): Boolean =
        info.type == FormVolunteerTypeUi.General ||
            (info.specificArea ?: fallbackSpecificArea()) != null

    // Area used by a specific shift when there was no picker: the single selectable area.
    // On a forced-online day that's the single ONLINE area, not the user's first area.
    private fun fallbackSpecificArea(): SpecificArea? = specificAreas.value.singleOrNull()

    private fun buildReservation(): Volunteer {
        // Selections are pruned as options are withdrawn; re-checked so a stale one never slips in
        val options = additionalOptionsSelected.value.filter { it in additionalOptions.value }
        return Volunteer(
            id = VolunteerId.Empty,
            userId = UserId.Empty,
            date = date.value!!,
            shifts = shiftsInfo.value.map { it.toDomainModel(fallbackSpecificArea()) },
            meals = options.getMeals(),
            sleep = options.contains(AdditionalOption.Sleep),
        )
    }

    private fun navigateBack() {
        _uiState.update { it.copy(isFormSavedSuccessfully = true) }
    }

    // A shift is online only if the user asked for it AND it is a specific shift in an area
    // that allows remote work; the toggle is hidden otherwise, but the state is re-checked
    // here so a stale value can never be persisted.
    private fun isOnline(
        online: Boolean,
        type: FormVolunteerTypeUi,
        specificArea: SpecificArea?,
        fallbackArea: SpecificArea? = fallbackSpecificArea(),
    ) = online &&
        type == FormVolunteerTypeUi.Specific &&
        areaConfig.value.allowsOnline(specificArea ?: fallbackArea)

    // The first other confirmed shift whose time range overlaps the given one's
    private fun List<ShiftInfoSummary>.overlapping(editor: ShiftEditor): ShiftInfoSummary? =
        firstOrNull { it.id != editor.editingId && it.timeRange.overlaps(editor.timeRange) }

    private fun List<ShiftInfoSummary>.overlapping(info: ShiftInfoSummary): ShiftInfoSummary? =
        firstOrNull { it.id != info.id && it.timeRange.overlaps(info.timeRange) }

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

    private fun ShiftInfoSummary.toDomainModel(fallbackArea: SpecificArea?): Shift {
        val volunteerType = type.toDomainModel(specificArea ?: fallbackArea)
        val online = isOnline(online, type, specificArea, fallbackArea)
        return when (shift) {
            ShiftUi.Morning -> Shift.Morning(volunteerType, timeRange, online)
            ShiftUi.Afternoon -> Shift.Afternoon(volunteerType, timeRange, online)
        }
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
    // A confirm is in flight: further taps on CONFIRMAR are ignored
    val isSaving: Boolean = false,
    val error: ReservationFormError? = null,
)

/** Why CONFIRMAR couldn't save the booking; shown to the user in a dialog. */
enum class ReservationFormError {
    MissingDate,
    MissingShift,
    // Meals or a bed can be booked without a shift, so any of them would do
    MissingShiftOrOption,
    MissingSpecificArea,
    OverlappingShifts,
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

/** The shift being added (`editingId` null) or edited in the modal. */
data class ShiftEditor(
    val editingId: Long?,
    val shift: ShiftUi,
    val type: FormVolunteerTypeUi,
    val timeRange: TimeRange,
    val specificArea: SpecificArea? = null,
    val online: Boolean = false,
)

/** Dialog shown when the chosen day has no on-site volunteering ("NO VOLUNTARIAT"). */
enum class RemoteDialog {
    None,
    /** The user has at least one online-capable area: they may continue, online only. */
    CanGoOnline,
    /** None of the user's areas allows remote work: the day can't be booked. */
    NoOnlineAreas,
}

data class ShiftInfoSummary(
    val id: Long,
    val shift: ShiftUi,
    val timeRange: TimeRange,
    val type: FormVolunteerTypeUi,
    val specificArea: SpecificArea? = null,
    val online: Boolean = false,
)

private const val LOG_TAG = "ReservationFormViewModel"

/** The user's own bookings on the days before and after the chosen date. */
private data class NeighbourBookings(
    val previousDay: Volunteer? = null,
    val nextDay: Volunteer? = null,
)
