package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.features.calendar.navigation.DayDetailNavKey
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.UserRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.collections.emptyList
import kotlin.time.Clock

class DayDetailViewModel(
    navKey: DayDetailNavKey,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val volunteerRepository: VolunteerRepository,
    calendarRepository: CalendarRepository,
) : ViewModel() {
    private val date = navKey.date

    private val _uiState = MutableStateFlow(
        DayDetailUiState(headerUi = DetailHeaderUi(date = date.format("dd MMMM"))),
    )
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    val events: StateFlow<List<GoogleCalendarEvent>> =
        calendarRepository
            .getGoogleCalendarEventsByDate(date)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyList(),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val thereIsProtectors: StateFlow<Boolean> =
        events
            .map { events ->
                events.any { it.title.contains("puja protectors", ignoreCase = true) }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = false,
            )
    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY

    // The reservation form only accepts today or later, so past days offer no «+» button.
    val canAddVolunteering: Boolean =
        date >= Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    private var selectedVolunteerToDelete: VolunteerId? = null

    init {
        refreshVolunteers()
    }

    fun onDeleteVolunteer(id: VolunteerId) {
        selectedVolunteerToDelete = id
        _showDeleteDialog.update { true }
    }

    fun onCloseDeleteDialog() {
        selectedVolunteerToDelete = null
        _showDeleteDialog.update { false }
    }

    fun deleteVolunteer() {
        if (selectedVolunteerToDelete == null) {
            return
        }
        viewModelScope.launch {
            volunteerRepository
                .deleteVolunteer(selectedVolunteerToDelete!!)
                .onSuccess {
                    refreshVolunteers()
                    onCloseDeleteDialog()
                }.onFailure {
                    // TODO: Handle the failure
                }
        }
    }

    private fun refreshVolunteers() {
        viewModelScope.launch {
            volunteerRepository
                .getVolunteersByDate(date)
                .mapCatching { volunteers ->
                    val users = userRepository.getAllUsers().getOrElse { emptyList() }
                    val currentUser = authRepository.getCurrentUser().getOrElse { null }
                    Triple(volunteers, users, currentUser)
                }.onSuccess { (volunteers, users, currentUser) ->
                    // Without a resolved viewer we can't apply the visibility rules, so
                    // nothing is shown rather than leaking other people's bookings.
                    val visibleVolunteers = if (currentUser == null) {
                        emptyList()
                    } else {
                        volunteers.filter { it.isVisibleTo(currentUser) }
                    }
                    _uiState.update {
                        it.copy(
                            dayShifts = buildDayShifts(visibleVolunteers, users, currentUser),
                            headerUi = it.headerUi.copy(numOfVolunteers = visibleVolunteers.size),
                        )
                    }
                }
        }
    }

    // One entry per shift: a volunteer booked for both morning and afternoon appears
    // under each heading (both entries share the booking id, so deleting either
    // removes the whole day's booking).
    private fun buildDayShifts(
        volunteers: List<Volunteer>,
        users: List<User>,
        viewer: User?,
    ) = DayShifts(
        morningVolunteers = volunteers.toShiftItems<Shift.Morning>(users, viewer),
        afternoonVolunteers = volunteers.toShiftItems<Shift.Afternoon>(users, viewer),
    )

    private inline fun <reified T : Shift> List<Volunteer>.toShiftItems(
        users: List<User>,
        viewer: User?,
    ): List<VolunteerItemUi> =
        mapNotNull { volunteer ->
            val shift = volunteer.shifts.firstOrNull { it is T } ?: return@mapNotNull null
            users
                .find { user -> user.id == volunteer.userId }
                ?.let { user -> volunteer.toUiModel(user.name, viewer, shift) }
        }
}

data class DayDetailUiState(
    val headerUi: DetailHeaderUi,
    val dayShifts: DayShifts = DayShifts(),
)

data class DetailHeaderUi(
    val date: String = "",
    val numOfVolunteers: Int = 0,
)

data class DayShifts(
    val morningVolunteers: List<VolunteerItemUi> = emptyList(),
    val afternoonVolunteers: List<VolunteerItemUi> = emptyList(),
)

data class VolunteerItemUi(
    val id: VolunteerId,
    val name: String,
    val schedule: String,
    val type: VolunteerType,
    val online: Boolean,
    val meals: List<Meal>,
    val sleep: Boolean,
    val canBeDeleted: Boolean,
)

private fun Volunteer.toUiModel(name: String, viewer: User?, shift: Shift): VolunteerItemUi {
    val showsServices = viewer != null && areServicesVisibleTo(viewer)
    return VolunteerItemUi(
        id = id,
        name = name,
        schedule = shift.formatSchedule(),
        type = shift.type,
        online = shift.online,
        meals = if (showsServices) meals else emptyList(),
        sleep = showsServices && sleep,
        canBeDeleted = viewer != null && isOwnedBy(viewer),
    )
}

private fun Shift.formatSchedule() =
    "${timeRange.start.format("HH:mm")} - ${timeRange.end.format("HH:mm")}"
