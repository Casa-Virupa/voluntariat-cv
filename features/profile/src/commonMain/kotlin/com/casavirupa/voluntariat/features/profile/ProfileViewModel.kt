package com.casavirupa.voluntariat.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun logOut() {
        viewModelScope.launch {
            authRepository
                .logOut()
                .onSuccess {
                    _uiState.update { it.copy(navigateToSignIn = true) }
                }.onFailure {
                    // TODO: Handle log out error
                }
        }
    }
}

data class ProfileUiState(val navigateToSignIn: Boolean = false)