package com.casavirupa.voluntariat.features.authentication.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LogInViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LogInUiState())
    val uiState: StateFlow<LogInUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _email.update { newEmail }
    }

    fun onPasswordChanged(newPassword: String) {
        _password.update { newPassword }
    }

    fun onLogIn() {
        viewModelScope.launch {
            authRepository
                .signIn(email.value, password.value)
                .onSuccess {
                    _uiState.update { it.copy(isLoggedIn = true) }
                }.onFailure {
                    Logger.e(LOG_TAG) { "Error on log in: ${it.cause}" }
                }
        }
    }
}

data class LogInUiState(
    val isLoggedIn: Boolean = false,
)

private const val LOG_TAG = "LogInViewModel"