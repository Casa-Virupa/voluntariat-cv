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

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _showInformationDialog = MutableStateFlow(false)
    val showInformationDialog: StateFlow<Boolean> = _showInformationDialog.asStateFlow()

    fun onEmailChanged(email: String) {
        _email.update { email }
    }

    fun resetPassword() {
        viewModelScope.launch {
            authRepository
                .resetPassword(email.value)
                .onSuccess {
                    showInformationDialog()
                }.onFailure { error ->
                    Logger.e(error, LOG_TAG) { "Error on reset password: ${error.message}" }
                }
        }
    }

    fun closeInformationDialog() {
        _showInformationDialog.update { false }
    }

    private fun showInformationDialog() {
        _showInformationDialog.update { true }
    }


    companion object {
        private const val LOG_TAG = "ForgotPasswordViewModel"
    }
}