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

    private val _forgotPassword = MutableStateFlow(ForgotPasswordUiState())
    val forgotPassword: StateFlow<ForgotPasswordUiState> = _forgotPassword.asStateFlow()

    private val _showCredentialsErrorDialog = MutableStateFlow(false)
    val showCredentialsErrorDialog: StateFlow<Boolean> = _showCredentialsErrorDialog.asStateFlow()

    fun onEmailChanged(newEmail: String) {
        _email.update { newEmail }
    }

    fun onPasswordChanged(newPassword: String) {
        _password.update { newPassword }
    }

    fun onSignIn() {
        viewModelScope.launch {
            authRepository
                .signIn(email.value, password.value)
                .onSuccess { user ->
                    when {
                        !user.hasOnboardingCompleted -> navigateToCreatePassword()
                        else -> navigateToSchedule()
                    }
                }.onFailure { error ->
                    Logger.e(error, LOG_TAG) { "Error on log in: ${error.message}" }
                    _showCredentialsErrorDialog.update { true }
                }
        }
    }

    fun onForgotPasswordClicked() {
        _forgotPassword.update {
            ForgotPasswordUiState(isVisible = true, email = email.value)
        }
    }

    fun onForgotPasswordEmailChanged(newEmail: String) {
        _forgotPassword.update { it.copy(email = newEmail, status = ForgotPasswordStatus.Idle) }
    }

    fun onSendPasswordResetEmail() {
        val resetEmail = _forgotPassword.value.email.trim()
        if (!isEmailValid(resetEmail)) {
            _forgotPassword.update { it.copy(status = ForgotPasswordStatus.InvalidEmail) }
            return
        }
        _forgotPassword.update { it.copy(status = ForgotPasswordStatus.Sending) }
        viewModelScope.launch {
            authRepository
                .sendPasswordResetEmail(resetEmail)
                .onSuccess {
                    _forgotPassword.update { it.copy(status = ForgotPasswordStatus.Sent) }
                }.onFailure { error ->
                    Logger.e(error, LOG_TAG) {
                        "Error sending password reset email: ${error.message}"
                    }
                    _forgotPassword.update { it.copy(status = ForgotPasswordStatus.Error) }
                }
        }
    }

    fun closeForgotPasswordDialog() {
        _forgotPassword.update { ForgotPasswordUiState() }
    }

    fun closeCredentialsErrorDialog() {
        _showCredentialsErrorDialog.update { false }
    }

    private fun navigateToCreatePassword() {
        _uiState.update { it.copy(navigateToCreatePassword = true, navigateToSchedule = false) }
    }

    private fun navigateToSchedule() {
        _uiState.update { it.copy(navigateToSchedule = true, navigateToCreatePassword = false) }
    }

    private fun isEmailValid(email: String) = EMAIL_REGEX.matches(email)
}

data class SignInUiState(
    val navigateToCreatePassword: Boolean = false,
    val navigateToSchedule: Boolean = false,
)

data class ForgotPasswordUiState(
    val isVisible: Boolean = false,
    val email: String = "",
    val status: ForgotPasswordStatus = ForgotPasswordStatus.Idle,
)

enum class ForgotPasswordStatus {
    Idle,
    InvalidEmail,
    Sending,
    Sent,
    Error,
}

private val EMAIL_REGEX = """^[^@\s]+@[^@\s]+\.[^@\s]+$""".toRegex()

private const val LOG_TAG = "LogInViewModel"
