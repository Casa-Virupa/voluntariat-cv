package com.casavirupa.voluntariat.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable.start
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
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.math.abs
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
                hours = it.shift.getHour().toInt(),
                shift = it.shift,
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
            summary = MonthSummary(0.0, 0),
            volunteers = emptyList(),
        )
    }
}

data class MonthSummary(
    val days: Double,
    val hours: Int,
) {
    companion object {
        operator fun invoke(volunteers: List<Volunteer>): MonthSummary =
            MonthSummary(
                days = volunteers.sumOf { it.shift.getHour() }.div(ALL_DAY_DIVIDER),
                hours = volunteers.sumOf { it.shift.getHour() }.toInt(),
            )
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
        is Shift.Morning -> this.timeRange.start - this.timeRange.end
        is Shift.Afternoon -> this.timeRange.start - this.timeRange.end
        is Shift.AllDay -> {
            (this.morningTimeRange.start - this.morningTimeRange.end) +
                    (this.afternoonTimeRange.start - this.afternoonTimeRange.end)
        }
        else -> 0.0
    }

private operator fun LocalTime.minus(other: LocalTime): Double {
    val diffSeconds = this.toSecondOfDay() - other.toSecondOfDay()
    return abs(diffSeconds) / 3600.0
}

private const val ALL_DAY_DIVIDER = 8.0