package com.casavirupa.voluntariat.shared.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _initialUserState = MutableStateFlow(InitialUserState.NotLogged)
    val initialUserState: StateFlow<InitialUserState> = _initialUserState.asStateFlow()

    init {
        viewModelScope.launch {
            _initialUserState.value =
                if (authRepository.isLoggedIn()) {
                    InitialUserState.LoggedIn
                } else {
                    InitialUserState.NotLogged
                }
        }
    }
}

enum class InitialUserState {
    LoggedIn,
    NotLogged
}