package com.casavirupa.voluntariat.features.authentication.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun onEmailChanged(email: String) {
        _email.update { email }
    }

    fun resetPassword() {
        viewModelScope.launch {
            authRepository
                .resetPassword(email.value)
                .onSuccess {

                }.onFailure {

                }
        }
    }
}