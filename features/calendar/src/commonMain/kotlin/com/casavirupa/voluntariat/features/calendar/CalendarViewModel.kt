package com.casavirupa.voluntariat.features.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.features.calendar.models.YearMonth
import com.casavirupa.voluntariat.shared.core.utils.getCurrentMonth
import com.casavirupa.voluntariat.shared.core.utils.getCurrentYear
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.model.calendar.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Month
import kotlinx.datetime.number

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
    private val _yearMonth = MutableStateFlow(YearMonth(getCurrentYear(), getCurrentMonth()))
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    init {
        viewModelScope.launch {
            yearMonth.collectLatest { yearMonth ->
                loadEvents(yearMonth.month.number)
            }
        }
    }

    fun onNextMonth() {
        _yearMonth.update { yearMonth ->
            if (isLastMonth(yearMonth.month)) {
                yearMonth.copy(
                    year = yearMonth.nextYear,
                    month = Month(FIRST_MONTH_NUMBER)
                )
            } else {
                yearMonth.copy(month = yearMonth.nextMonth)
            }
        }
    }

    fun onPreviousMonth() {
        _yearMonth.update { yearMonth ->
            if (isFirstMonth(yearMonth.month)) {
                yearMonth.copy(
                    year = yearMonth.prevYear,
                    month = Month(LAST_MONTH_NUMBER)
                )
            } else {
                yearMonth.copy(month = yearMonth.prevMonth)
            }
        }
    }

    private fun loadEvents(monthNumber: Int) {
        viewModelScope.launch {
            calendarRepository
                .getCalendarEvents(monthNumber = monthNumber)
                .onSuccess { events ->
                    _events.update { events }
                }.onFailure {
                    // TODO: Handle error
                }
        }
    }

    private fun isLastMonth(month: Month) = month.number == Month.entries.last().number

    private fun isFirstMonth(month: Month) = month.number == Month.entries.first().number
}

private const val FIRST_MONTH_NUMBER = 1
private const val LAST_MONTH_NUMBER = 12
