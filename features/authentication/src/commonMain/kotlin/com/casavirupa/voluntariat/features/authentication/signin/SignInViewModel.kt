package com.casavirupa.voluntariat.features.authentication.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

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
                    _uiState.update { it.copy(isSignedIn = true) }
                }.onFailure { error ->
                    Logger.e(LOG_TAG) { "Error on log in: ${error.cause}" }
                }
        }
    }
}

data class SignInUiState(
    val isSignedIn: Boolean = false,
)

private const val LOG_TAG = "LogInViewModel"