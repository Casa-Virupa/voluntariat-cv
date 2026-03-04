package com.casavirupa.voluntariat.features.authentication.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CreatePasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePasswordUiState())
    val uiState: StateFlow<CreatePasswordUiState> = _uiState.asStateFlow()

    private val _actualPassword = MutableStateFlow("")
    val actualPassword: StateFlow<String> = _actualPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmNewPassword = MutableStateFlow("")
    val confirmNewPassword: StateFlow<String> = _confirmNewPassword.asStateFlow()

    fun onActualPasswordChanged(actualPassword: String) {
        _actualPassword.update { actualPassword }
    }

    fun onNewPasswordChanged(newPassword: String) {
        _newPassword.update { newPassword }
    }

    fun onConfirmNewPasswordChanged(confirmNewPassword: String) {
        _confirmNewPassword.update { confirmNewPassword }
        if (_uiState.value.showPasswordNotMatchError) {
            showPasswordNotMatchError(false)
        }
    }

    fun onCreatePassword() {
        if (newPassword.value != confirmNewPassword.value) {
            showPasswordNotMatchError(true)
            return
        }
        viewModelScope.launch {
            createNewPassword()
        }
    }

    private fun showPasswordNotMatchError(visibility: Boolean) {
        _uiState.update { it.copy(showPasswordNotMatchError = visibility) }
    }

    private suspend fun createNewPassword() {
        authRepository
            .updateNewPassword(actualPassword.value, newPassword.value)
            .onSuccess {
                navigateToSchedule()
            }.onFailure { error ->
                Logger.e(LOG_TAG) { "Error on create password: ${error.cause}" }
            }
    }

    private fun navigateToSchedule() {
        _uiState.update { it.copy(navigateToSchedule = true) }
    }
}

data class CreatePasswordUiState(
    val navigateToSchedule: Boolean = false,
    val showPasswordNotMatchError: Boolean = false,
)

private const val LOG_TAG = "CreatePasswordViewModel"