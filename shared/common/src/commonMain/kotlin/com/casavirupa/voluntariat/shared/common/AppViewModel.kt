package com.casavirupa.voluntariat.shared.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _initialUserState = MutableStateFlow<InitialUserState?>(null)
    val initialUserState: StateFlow<InitialUserState?> = _initialUserState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1000) // Fake delay
            _initialUserState.value =
                if (authRepository.isLoggedIn()) {
                    InitialUserState.LoggedIn(onboardingCompleted = false)
                } else {
                    InitialUserState.NotLogged
                }
        }
    }
}

sealed class InitialUserState {
    data class LoggedIn(val onboardingCompleted: Boolean) : InitialUserState()
    object NotLogged : InitialUserState()
}