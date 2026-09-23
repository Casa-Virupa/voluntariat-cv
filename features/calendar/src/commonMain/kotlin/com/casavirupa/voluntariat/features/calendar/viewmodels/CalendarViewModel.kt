package com.casavirupa.voluntariat.features.calendar.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.features.calendar.models.YearMonth
import com.casavirupa.voluntariat.shared.core.utils.getCurrentMonth
import com.casavirupa.voluntariat.shared.core.utils.getCurrentYear
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.CalendarFilter
import com.casavirupa.voluntariat.shared.model.calendar.DayEvents
import com.casavirupa.voluntariat.shared.model.calendar.groupByDay
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
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
    authRepository: AuthRepository,
) : ViewModel() {
    private val _yearMonth = MutableStateFlow(YearMonth(getCurrentYear(), getCurrentMonth()))
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    private val _filter = MutableStateFlow<CalendarFilter?>(null)
    val filter: StateFlow<CalendarFilter?> = _filter.asStateFlow()

    private val currentUser: StateFlow<User?> =
        authRepository
            .getCurrentUserFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = null,
            )

    // Every booking from today on, in one realtime listener that lives as long as the screen is
    // subscribed. Changing month only changes which slice of it the grid shows, so swiping never
    // recreates the listener nor re-downloads the collection.
    private val upcomingVolunteers: Flow<List<Volunteer>> =
        volunteerRepository
            .getVolunteersFrom(todayDate)
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                replay = 1,
            )

    // Per-day count of the volunteers the viewer is allowed to see (same rules as the day
    // detail), narrowed further by the active filter. Nothing is counted until the viewer
    // is known, so no booking is exposed by mistake.
    val volunteers: StateFlow<Map<LocalDate, Int>> =
        combine(upcomingVolunteers, currentUser, filter) { volunteers, user, filter ->
            if (user == null) return@combine emptyMap()
            volunteers
                // A booking with only meals or a bed isn't a volunteer on that day
                .filter { it.shifts.isNotEmpty() }
                .filter { it.isVisibleTo(user) && (filter == null || filter.matches(it, user)) }
                .groupBy { it.date }
                .mapValues { (_, volunteers) -> volunteers.size }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyMap(),
        )

    // Days with a booking of the viewer's own, highlighted regardless of the active filter.
    val myVolunteerDates: StateFlow<Set<LocalDate>> =
        combine(upcomingVolunteers, currentUser) { volunteers, user ->
            if (user == null) return@combine emptySet()
            volunteers.filter { it.isOwnedBy(user) }.map { it.date }.toSet()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptySet(),
        )

    // Every cached event, indexed by day once per database emission (off the main thread) so a
    // pager page only looks its 42 days up. Covering all months at once means the neighbour
    // page is already drawn with its events while swiping.
    val eventsByDay: StateFlow<Map<LocalDate, DayEvents>> =
        calendarRepository
            .getGoogleCalendarEvents()
            .map { events -> events.groupByDay() }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = emptyMap(),
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

    // Tapping the active filter again clears it.
    fun onFilterSelected(selected: CalendarFilter) {
        _filter.update { current -> if (current == selected) null else selected }
    }

    /**
     * Called whenever [month] is shown (entering the screen, changing month, returning to the
     * foreground): refreshes its Google Calendar events and its neighbours' if they are stale.
     */
    fun onMonthShown(month: YearMonth) {
        viewModelScope.launch {
            calendarRepository.syncGoogleCalendarEventsAround(
                year = month.year,
                month = month.month.number,
            )
        }
    }
}
