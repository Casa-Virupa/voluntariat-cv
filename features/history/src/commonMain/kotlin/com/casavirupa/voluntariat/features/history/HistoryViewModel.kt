package com.casavirupa.voluntariat.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.UserRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.time.Clock

class HistoryViewModel(
    volunteerRepository: VolunteerRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    private val _currentDate = MutableStateFlow(Clock.System.now().toDate())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState =
        combine(
            currentDate,
            authRepository.getCurrentUserFlow()
        ) { date, user ->
            date to user
        }.flatMapLatest { (date, user) ->
            volunteerRepository.getVolunteersByUserAndMonth(
                id = user.id,
                monthNumber = date.month.number,
                year = date.year
            )
        }.map { volunteers ->
            volunteers.toUiState()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HistoryUiState.Empty,
        )

    fun nextMonth() {
        _currentDate.update { it.plus(DatePeriod(months = 1)) }
    }

    fun previousMonth() {
        _currentDate.update { it.minus(DatePeriod(months = 1)) }
    }

    private fun List<Volunteer>.toUiState() =
        HistoryUiState(
            summary = MonthSummary(this),
            volunteers = toUiModel()
        )

    private fun List<Volunteer>.toUiModel() =
        map {
            VolunteerHistoryItem(
                date = it.date,
                hours = it.volunteerShift.getHour(),
                shift = it.volunteerShift,
                meals = it.meals,
            )
        }
}

data class HistoryUiState(
    val summary: MonthSummary,
    val volunteers: List<VolunteerHistoryItem>,
) {
    companion object {
        val Empty = HistoryUiState(
            summary = MonthSummary(0, 0),
            volunteers = emptyList(),
        )
    }
}

data class MonthSummary(
    val hours: Int,
    val days: Int,
) {
    companion object {
        operator fun invoke(volunteers: List<Volunteer>): MonthSummary =
            MonthSummary(
                days = volunteers.map { it.date }.distinct().count(),
                hours = volunteers.map { it.volunteerShift }.calculateHours(),
            )

        private fun List<Shift>.calculateHours() = this.sumOf { it.getHour() }
    }
}

data class VolunteerHistoryItem(
    val date: LocalDate,
    val hours: Int,
    val shift: Shift,
    val meals: List<Meal>
)

private fun Shift.getHour() =
    when (this) {
        Shift.Morning -> HALF_JOURNEY
        Shift.Afternoon -> HALF_JOURNEY
        Shift.AllDay -> ALL_DAY_JOURNEY
        else -> 0
    }

private const val HALF_JOURNEY = 4
private const val ALL_DAY_JOURNEY = 8