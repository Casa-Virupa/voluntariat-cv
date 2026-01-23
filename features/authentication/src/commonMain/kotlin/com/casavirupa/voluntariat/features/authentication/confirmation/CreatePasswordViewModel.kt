package com.casavirupa.voluntariat.features.authentication.confirmation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CreatePasswordViewModel : ViewModel() {
    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmNewPassword = MutableStateFlow("")
    val confirmNewPassword: StateFlow<String> = _confirmNewPassword.asStateFlow()

    fun onNewPasswordChanged(newPassword: String) {
        _newPassword.value = newPassword
    }

    fun onConfirmNewPasswordChanged(confirmNewPassword: String) {
        _confirmNewPassword.value = confirmNewPassword
    }
}