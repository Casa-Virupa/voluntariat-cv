package com.casavirupa.voluntariat.shared.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.number
import kotlin.time.Clock

class AppViewModel(
    private val authRepository: AuthRepository,
    private val calendarRepository: CalendarRepository,
) : ViewModel() {
    private val _initialUserState = MutableStateFlow<InitialUserState?>(null)
    val initialUserState: StateFlow<InitialUserState?> = _initialUserState.asStateFlow()

    init {
        // Warm the local cache of Google Calendar events around today. It runs on its own so a
        // slow Apps Script never delays the login decision below.
        viewModelScope.launch {
            val today = Clock.System.now().toDate()
            calendarRepository.syncGoogleCalendarEventsAround(
                year = today.year,
                month = today.month.number,
            )
        }
        viewModelScope.launch {
            authRepository
                .getCurrentUser()
                .onSuccess {
                    _initialUserState.value = InitialUserState.LoggedIn(it.hasOnboardingCompleted)
                }.onFailure {
                    _initialUserState.value = InitialUserState.NotLogged
                }
        }
    }
}

sealed class InitialUserState {
    data class LoggedIn(val onboardingCompleted: Boolean) : InitialUserState()
    object NotLogged : InitialUserState()
}
