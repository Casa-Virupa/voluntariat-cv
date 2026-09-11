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
import kotlinx.datetime.LocalTime
import kotlin.collections.emptyList

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

    private fun buildDayShifts(
        volunteers: List<Volunteer>,
        users: List<User>,
        viewer: User?,
    ) = DayShifts(
        allDayVolunteers = volunteers
            .filter { it.shifts.size > 1 }
            .mapNotNull { volunteer ->
                users
                    .find { user -> user.id == volunteer.userId }
                    ?.let { user ->
                        volunteer.toUiModel(user.name, viewer)
                    }
            },
        morningVolunteers = volunteers
            .filter { it.shifts.all { shift -> shift is Shift.Morning } }
            .mapNotNull { volunteer ->
                users
                    .find { user -> user.id == volunteer.userId }
                    ?.let { user ->
                        volunteer.toUiModel(user.name, viewer)
                    }
            },
        afternoonVolunteers = volunteers
            .filter { it.shifts.all { shift -> shift is Shift.Afternoon } }
            .mapNotNull { volunteer ->
                users
                    .find { user -> user.id == volunteer.userId }
                    ?.let { user ->
                        volunteer.toUiModel(user.name, viewer)
                    }
            },
    )
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
    val allDayVolunteers: List<VolunteerItemUi> = emptyList(),
    val morningVolunteers: List<VolunteerItemUi> = emptyList(),
    val afternoonVolunteers: List<VolunteerItemUi> = emptyList(),
)

data class VolunteerItemUi(
    val id: VolunteerId,
    val name: String,
    val schedule: String,
    val type: VolunteerTypeUi,
    val meals: List<Meal>,
    val sleep: Boolean,
    val canBeEdited: Boolean,
)

sealed class VolunteerTypeUi {
    data class Single(
        val type: VolunteerType,
        val online: Boolean = false,
    ) : VolunteerTypeUi()

    data class AllDay(
        val morning: VolunteerType,
        val afternoon: VolunteerType,
        val morningOnline: Boolean = false,
        val afternoonOnline: Boolean = false,
    ) : VolunteerTypeUi()
}

private fun Volunteer.toUiModel(name: String, viewer: User?): VolunteerItemUi {
    val showsServices = viewer != null && areServicesVisibleTo(viewer)
    return VolunteerItemUi(
        id = id,
        name = name,
        schedule = shifts.formatSchedule(),
        type = if (shifts.size > 1) {
            buildAllDayVolunteerType(shifts)
        } else {
            buildSingleVolunteerType(shifts.first())
        },
        meals = if (showsServices) meals else emptyList(),
        sleep = showsServices && sleep,
        canBeEdited = viewer != null && isOwnedBy(viewer),
    )
}

private fun buildAllDayVolunteerType(shifts: List<Shift>): VolunteerTypeUi.AllDay {
    val morning = shifts.first { it.timeRange.end <= LocalTime(14, 0) }
    val afternoon = shifts.first { it.timeRange.end > LocalTime(14, 0) }
    return VolunteerTypeUi.AllDay(
        morning = morning.type,
        afternoon = afternoon.type,
        morningOnline = morning.online,
        afternoonOnline = afternoon.online,
    )
}

private fun buildSingleVolunteerType(volunteerShift: Shift) =
    VolunteerTypeUi.Single(type = volunteerShift.type, online = volunteerShift.online)

private fun List<Shift>.formatSchedule() =
    sortedBy { it.timeRange.start }
        .joinToString(" · ") { shift ->
            "${shift.timeRange.start.format("HH:mm")} - ${shift.timeRange.end.format("HH:mm")}"
        }
