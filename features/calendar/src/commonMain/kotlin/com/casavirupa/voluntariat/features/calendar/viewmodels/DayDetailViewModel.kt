package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.features.calendar.navigation.DayDetailNavKey
import com.casavirupa.voluntariat.shared.core.utils.format
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.UserRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.SpecificArea
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

class DayDetailViewModel(
    navKey: DayDetailNavKey,
    private val calendarRepository: CalendarRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val date = navKey.date

    private val _uiState = MutableStateFlow(
        DayDetailUiState(headerUi = DetailHeaderUi(date = date.format("dd MMMM"))),
    )
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
           calendarRepository
               .getVolunteersByDate(date)
               .mapCatching { volunteers ->
                   val users = userRepository.getAllUsers().getOrElse { emptyList() }
                   volunteers to users
               }.onSuccess { (volunteers, users) ->
                   _uiState.update { it.copy(dayShifts = buildDayShifts(volunteers, users)) }
               }
        }
    }

    private fun buildDayShifts(volunteers: List<Volunteer>, users: List<User>) =
        DayShifts(
            allDayVolunteers = volunteers
                .filter { it.volunteerShift == Shift.AllDay }
                .mapNotNull { volunteer ->
                    users
                        .find { user -> user.id == volunteer.userId }
                        ?.let { user ->
                            volunteer.toUiModel(user.name)
                        }
                },
            morningVolunteers = volunteers
                .filter { it.volunteerShift == Shift.Morning }
                .mapNotNull { volunteer ->
                    users
                        .find { user -> user.id == volunteer.userId }
                        ?.let { user ->
                            volunteer.toUiModel(user.name)
                        }
                },
            afternoonVolunteers = volunteers
                .filter { it.volunteerShift == Shift.Afternoon }
                .mapNotNull { volunteer ->
                    users
                        .find { user -> user.id == volunteer.userId }
                        ?.let { user ->
                            volunteer.toUiModel(user.name)
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
    val name: String,
    val type: VolunteerTypeUi,
    val meals: List<Meal>,
    val sleep: Boolean,
)

sealed class VolunteerTypeUi {
    data class Single(val type: VolunteerType) : VolunteerTypeUi()

    data class AllDay(
        val morning: VolunteerType,
        val afternoon: VolunteerType,
    ) : VolunteerTypeUi()
}

private fun Volunteer.toUiModel(name: String) =
    if (volunteerShift == Shift.AllDay) {
        VolunteerItemUi(
            name = name,
            type = buildAllDayVolunteerType(specificArea),
            meals = listOf(meal),
            sleep = sleep,
        )
    } else {
        VolunteerItemUi(
            name = name,
            type = buildSingleVolunteerType(specificArea, volunteerShift),
            meals = listOf(meal),
            sleep = sleep,
        )
    }

private fun buildAllDayVolunteerType(specificArea: SpecificArea) =
    VolunteerTypeUi.AllDay(
        morning = setMorningVolunteerType(specificArea),
        afternoon = setAfternoonVolunteerType(specificArea),
    )

private fun buildSingleVolunteerType(
    specificArea: SpecificArea,
    volunteerShift: Shift,
) =
    VolunteerTypeUi.Single(
        type = when (volunteerShift) {
            Shift.Morning -> setMorningVolunteerType(specificArea)
            Shift.Afternoon -> setAfternoonVolunteerType(specificArea)
            else -> VolunteerType.General
        }
    )

private fun setMorningVolunteerType(specificArea: SpecificArea): VolunteerType =
    if (specificArea == SpecificArea.Morning) {
        VolunteerType.Specific
    } else {
        VolunteerType.General
    }

private fun setAfternoonVolunteerType(specificArea: SpecificArea): VolunteerType =
    if (specificArea == SpecificArea.Afternoon) {
        VolunteerType.Specific
    } else {
        VolunteerType.General
    }
