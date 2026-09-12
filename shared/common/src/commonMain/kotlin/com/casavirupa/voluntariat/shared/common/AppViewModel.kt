package com.casavirupa.voluntariat.shared.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class AppViewModel(
    private val authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
    private val _initialUserState = MutableStateFlow<InitialUserState?>(null)
    val initialUserState: StateFlow<InitialUserState?> = _initialUserState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentDate = Clock.System.now().toDate()
            calendarRepository.syncGoogleCalendarEvents(
                year = currentDate.year,
                startMonth = currentDate.month.number,
                endMonth = calculateNextMonth(currentDate),
            )
            authRepository
                .getCurrentUser()
                .onSuccess {
                    _initialUserState.value = InitialUserState.LoggedIn(it.hasOnboardingCompleted)
                }.onFailure {
                    _initialUserState.value = InitialUserState.NotLogged
                }
        }
    }

    private fun calculateNextMonth(currentDate: LocalDate): Int {
        val nextMonth = currentDate.month.number + NEXT_MONTHS
        return if (currentDate.month.number + NEXT_MONTHS > MAX_MONTH_NUMBER) {
            nextMonth - MAX_MONTH_NUMBER
        } else {
            nextMonth
        }
    }

    companion object {
        private const val MAX_MONTH_NUMBER = 12
        private const val NEXT_MONTHS = 3
    }
}

sealed class InitialUserState {
    data class LoggedIn(val onboardingCompleted: Boolean) : InitialUserState()
    object NotLogged : InitialUserState()
}