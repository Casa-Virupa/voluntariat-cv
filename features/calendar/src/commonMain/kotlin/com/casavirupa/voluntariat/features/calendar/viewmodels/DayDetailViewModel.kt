package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.features.calendar.navigation.DayDetailNavKey
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.UserRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.SpecificArea
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication.Companion.init
import kotlin.collections.emptyList

class DayDetailViewModel(
    navKey: DayDetailNavKey,
    private val calendarRepository: CalendarRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val volunteerRepository: VolunteerRepository,
) : ViewModel() {
    private val date = navKey.date

    private val _uiState = MutableStateFlow(
        DayDetailUiState(headerUi = DetailHeaderUi(date = date.format("dd MMMM"))),
    )
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

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
            calendarRepository
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
            .filter { it.volunteerShift == Shift.AllDay }
            .mapNotNull { volunteer ->
                users
                    .find { user -> user.id == volunteer.userId }
                    ?.let { user ->
                        volunteer.toUiModel(user.name, currentUserId)
                    }
            },
        morningVolunteers = volunteers
            .filter { it.volunteerShift == Shift.Morning }
            .mapNotNull { volunteer ->
                users
                    .find { user -> user.id == volunteer.userId }
                    ?.let { user ->
                        volunteer.toUiModel(user.name, currentUserId)
                    }
            },
        afternoonVolunteers = volunteers
            .filter { it.volunteerShift == Shift.Afternoon }
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
    if (volunteerShift == Shift.AllDay) {
        VolunteerItemUi(
            id = id,
            name = name,
            type = buildAllDayVolunteerType(specificArea),
            meals = meals,
            sleep = sleep,
            canBeDeleted = this.userId == userId,
        )
    } else {
        VolunteerItemUi(
            id = id,
            name = name,
            type = buildSingleVolunteerType(specificArea, volunteerShift),
            meals = meals,
            sleep = sleep,
            canBeDeleted = this.userId == userId,
        )
    }

private fun buildAllDayVolunteerType(specificArea: SpecificArea?) =
    VolunteerTypeUi.AllDay(
        morning = setMorningVolunteerType(specificArea),
        afternoon = setAfternoonVolunteerType(specificArea),
    )

private fun buildSingleVolunteerType(
    specificArea: SpecificArea?,
    volunteerShift: Shift,
) =
    VolunteerTypeUi.Single(
        type = when (volunteerShift) {
            Shift.Morning -> setMorningVolunteerType(specificArea)
            Shift.Afternoon -> setAfternoonVolunteerType(specificArea)
            else -> VolunteerType.General
        }
    )

private fun setMorningVolunteerType(specificArea: SpecificArea?): VolunteerType =
    if (specificArea == SpecificArea.Morning) {
        VolunteerType.Specific
    } else {
        VolunteerType.General
    }

private fun setAfternoonVolunteerType(specificArea: SpecificArea?): VolunteerType =
    if (specificArea == SpecificArea.Afternoon) {
        VolunteerType.Specific
    } else {
        VolunteerType.General
    }
