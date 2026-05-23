package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.features.calendar.models.YearMonth
import com.casavirupa.voluntariat.shared.core.utils.getCurrentMonth
import com.casavirupa.voluntariat.shared.core.utils.getCurrentYear
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val volunteerRepository: VolunteerRepository,
) : ViewModel() {
    private val _yearMonth = MutableStateFlow(YearMonth(getCurrentYear(), getCurrentMonth()))
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val volunteers: StateFlow<Map<LocalDate, Int>> =
        yearMonth
            .flatMapLatest {
                volunteerRepository
                    .getVolunteersByDateRange(
                        year = it.year,
                        monthNumber = it.month.number,
                        currentDate = todayDate,
                    )
            }.map { volunteers ->
                volunteers
                    .groupBy { it.date }
                    .mapValues { (_, volunteers) -> volunteers.size }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyMap(),
            )
    @OptIn(ExperimentalCoroutinesApi::class)
    val googleCalendarEvents: StateFlow<List<GoogleCalendarEvent>> =
        calendarRepository
            .getGoogleCalendarEvents()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyList(),
            )


    val todayDate
        get() = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

    fun onNextMonth() {
        _yearMonth.update { current ->
            current.copy(
                year = current.nextYear,
                month = current.nextMonth,
            )
        }
    }

    fun onPreviousMonth() {
        _yearMonth.update { current ->
            current.copy(
                year = current.prevYear,
                month = current.prevMonth,
            )
        }
    }

    fun onYearMonthChanged(newYearMonth: YearMonth) {
        _yearMonth.update { newYearMonth }
    }
}