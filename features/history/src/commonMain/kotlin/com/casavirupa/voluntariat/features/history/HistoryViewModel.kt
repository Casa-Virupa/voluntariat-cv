package com.casavirupa.voluntariat.features.history

import androidx.lifecycle.ViewModel
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Clock

class HistoryViewModel : ViewModel() {
    private val _currentDate = MutableStateFlow(Clock.System.now().toDate())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    val volunteers = MutableStateFlow(
        listOf(
            VolunteerHistoryItem(
                date = LocalDate(year = 2026, month = Month.MAY, day = 3),
                hours = 3,
                shift = Shift.Morning,
                meals = listOf(),
            ),
            VolunteerHistoryItem(
                date = LocalDate(year = 2026, month = Month.MAY, day = 4),
                hours = 4,
                shift = Shift.Afternoon,
                meals = listOf(),
            ),
            VolunteerHistoryItem(
                date = LocalDate(year = 2026, month = Month.MAY, day = 5),
                hours = 5,
                shift = Shift.AllDay,
                meals = listOf(),
            ),
        )
    ).asStateFlow()

    fun nextMonth() {
        _currentDate.update { it.plus(DatePeriod(months = 1)) }
    }

    fun previousMonth() {
        _currentDate.update { it.minus(DatePeriod(months = 1)) }
    }
}

data class MonthSummary(
    val hours: Int,
    val days: Int,
)

data class VolunteerHistoryItem(
    val date: LocalDate,
    val hours: Int,
    val shift: Shift,
    val meals: List<Meal>
)