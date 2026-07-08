package com.casavirupa.voluntariat.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.user.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.time.Clock

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val volunteerRepository: VolunteerRepository,
) : ViewModel() {
    private val _currentMonth = MutableStateFlow(Clock.System.now().toDate())
    val currentMonth: StateFlow<LocalDate> = _currentMonth.asStateFlow()

    private val _quarter = MutableStateFlow(Quarter.fromDate(_currentMonth.value))
    val quarter: StateFlow<Quarter> = _quarter.asStateFlow()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val user: StateFlow<User?> =
        authRepository
            .getCurrentUserFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = null,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val hoursDone: StateFlow<Int> =
        combine(user, currentMonth, quarter) { user, month, quarter ->
            Triple(user, month, quarter)
        }.flatMapLatest { (user, month, quarter) ->
            if (user == null) return@flatMapLatest kotlinx.coroutines.flow.flowOf(0)

            if (user.isMitra) {
                volunteerRepository.getVolunteersByUserAndQuarter(
                    id = user.id,
                    quarter = quarter.number,
                    year = quarter.year
                )
            } else {
                volunteerRepository.getVolunteersByUserAndMonth(
                    id = user.id,
                    monthNumber = month.month.number,
                    year = month.year
                )
            }.map { volunteers ->
                volunteers.sumOf { it.calculateHours() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0,
        )

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

    fun nextMonth() {
        _currentMonth.update { it.plus(DatePeriod(months = 1)) }
    }

    fun previousMonth() {
        _currentMonth.update { it.minus(DatePeriod(months = 1)) }
    }

    fun nextQuarter() {
        _quarter.update { it.nextQuarter() }
    }

    fun previousQuarter() {
        _quarter.update { it.previousQuarter() }
    }
}

data class ProfileUiState(val navigateToSignIn: Boolean = false)

data class Quarter(
    val number: Int,
    val year: Int,
) {
    fun nextQuarter(): Quarter {
        val num = if (number == 4) 1 else number + 1
        val year = if (number == 4) year + 1 else year
        return copy(number = num, year = year)
    }

    fun previousQuarter(): Quarter {
        val num = if (number == 1) 4 else number - 1
        val year = if (number == 1) year - 1 else year
        return copy(number = num, year = year)
    }

    companion object {
        fun fromDate(date: LocalDate) =
            Quarter(
                number = ((date.month.number + 2) / 3),
                year = date.year
            )
    }
}