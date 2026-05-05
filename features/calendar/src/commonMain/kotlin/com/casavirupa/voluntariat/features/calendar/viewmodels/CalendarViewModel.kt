package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.features.calendar.models.YearMonth
import com.casavirupa.voluntariat.shared.core.utils.getCurrentMonth
import com.casavirupa.voluntariat.shared.core.utils.getCurrentYear
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.GoogleCalendarEvent
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
    private val _yearMonth = MutableStateFlow(YearMonth(getCurrentYear(), getCurrentMonth()))
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    private val _volunteers = MutableStateFlow<List<Volunteer>>(emptyList())
    val volunteers: StateFlow<List<Volunteer>> = _volunteers.asStateFlow()

    private val _googleCalendarEvents = MutableStateFlow<List<GoogleCalendarEvent>>(emptyList())
    val googleCalendarEvents: StateFlow<List<GoogleCalendarEvent>> =
        _googleCalendarEvents.asStateFlow()


    val todayDate
        get() = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

    init {
        viewModelScope.launch {
            yearMonth.collectLatest { yearMonth ->
                loadEvents(yearMonth)
                loadGoogleCalendarEvents(yearMonth)
            }
        }
    }

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

    private fun loadEvents(yearMonth: YearMonth) {
        viewModelScope.launch {
            calendarRepository
                .getCalendarEvents(year = yearMonth.year, monthNumber = yearMonth.month.number)
                .onSuccess { events ->
                    _volunteers.update { events }
                    Logger.d("asdd") { events.toString() }
                }.onFailure {
                    // TODO: Handle error
                }
        }
    }

    private fun loadGoogleCalendarEvents(yearMonth: YearMonth) {
        viewModelScope.launch {
            calendarRepository
                .getGoogleCalendarEvents(
                    year = yearMonth.year,
                    monthNumber = yearMonth.month.number,
                ).onSuccess { events ->
                    _googleCalendarEvents.update { events }
                }.onFailure {
                    Logger.d("GoogleCalendarEvents") { it.message.toString() }
                    // TODO: Handle error
                }
        }
    }
}