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
import com.casavirupa.voluntariat.shared.model.user.UserId
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
                    _uiState.update {
                        it.copy(
                            dayShifts = buildDayShifts(volunteers, users, currentUser?.id),
                            headerUi = it.headerUi.copy(numOfVolunteers = volunteers.size),
                        )
                    }
                }
        }
    }

    private fun buildDayShifts(
        volunteers: List<Volunteer>,
        users: List<User>,
        currentUserId: UserId?,
    ) = DayShifts(
        allDayVolunteers = volunteers
            .filter { it.shifts.size > 1 }
            .mapNotNull { volunteer ->
                users
                    .find { user -> user.id == volunteer.userId }
                    ?.let { user ->
                        volunteer.toUiModel(user.name, currentUserId)
                    }
            },
        morningVolunteers = volunteers
            .filter { it.shifts.all { shift -> shift is Shift.Morning } }
            .mapNotNull { volunteer ->
                users
                    .find { user -> user.id == volunteer.userId }
                    ?.let { user ->
                        volunteer.toUiModel(user.name, currentUserId)
                    }
            },
        afternoonVolunteers = volunteers
            .filter { it.shifts.all { shift -> shift is Shift.Afternoon } }
            .mapNotNull { volunteer ->
                users
                    .find { user -> user.id == volunteer.userId }
                    ?.let { user ->
                        volunteer.toUiModel(user.name, currentUserId)
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
    val type: VolunteerTypeUi,
    val meals: List<Meal>,
    val sleep: Boolean,
    val canBeDeleted: Boolean,
)

sealed class VolunteerTypeUi {
    data class Single(val type: VolunteerType) : VolunteerTypeUi()

    data class AllDay(
        val morning: VolunteerType,
        val afternoon: VolunteerType,
    ) : VolunteerTypeUi()
}

private fun Volunteer.toUiModel(name: String, userId: UserId?) =
    VolunteerItemUi(
        id = id,
        name = name,
        type = if (shifts.size > 1) {
            buildAllDayVolunteerType(shifts)
        } else {
            buildSingleVolunteerType(shifts.first())
        },
        meals = meals,
        sleep = sleep,
        canBeDeleted = this.userId == userId,
    )

private fun buildAllDayVolunteerType(shifts: List<Shift>) =
    VolunteerTypeUi.AllDay(
        morning = shifts.first { it.timeRange.end <= LocalTime(14, 0) }.type,
        afternoon = shifts.first { it.timeRange.end > LocalTime(14, 0) }.type,
    )

private fun buildSingleVolunteerType(volunteerShift: Shift) =
    VolunteerTypeUi.Single(type = volunteerShift.type)
