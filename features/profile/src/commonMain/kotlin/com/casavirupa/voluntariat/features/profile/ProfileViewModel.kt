package com.casavirupa.voluntariat.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CommitmentRepository
import com.casavirupa.voluntariat.shared.domain.InterestLinksRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.commitment.CommitmentPeriod
import com.casavirupa.voluntariat.shared.model.commitment.CommitmentTarget
import com.casavirupa.voluntariat.shared.model.configuration.InterestLinks
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserRole
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
    interestLinksRepository: InterestLinksRepository,
    commitmentRepository: CommitmentRepository,
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

    val interestLinks: StateFlow<InterestLinks> =
        interestLinksRepository
            .getInterestLinks()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = InterestLinks.Empty,
            )

    // Target derived from the dashboard-published commitment_rules. Null means the
    // user has no commitment for the viewed period (show no target rather than 0 %).
    // The hardcoded defaults in User.getMonthHours are used only when the collection
    // could not be read at all (the repository emits null).
    val commitmentTarget: StateFlow<CommitmentTarget?> =
        combine(
            user,
            currentMonth,
            quarter,
            commitmentRepository.getCommitmentRules(),
        ) { user, month, quarter, rules ->
            if (user == null) return@combine null
            val viewedPeriod = if (user.isMitra) CommitmentPeriod.Quarter else CommitmentPeriod.Month
            val periodStart = if (user.isMitra) {
                quarter.startDate()
            } else {
                LocalDate(month.year, month.month, 1)
            }
            if (rules == null) {
                user.getMonthHours()
                    .takeIf { it > 0 }
                    ?.let { hours ->
                        CommitmentTarget(
                            targetMinutes = hours * MINUTES_PER_HOUR,
                            nativePeriod = viewedPeriod,
                            isScaled = false,
                        )
                    }
            } else {
                rules.targetFor(
                    uid = user.id,
                    volunteerType = (user.role as? UserRole.Volunteer)?.type,
                    periodStart = periodStart,
                    viewedPeriod = viewedPeriod,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val hoursDone: StateFlow<HoursBreakdown> =
        combine(user, currentMonth, quarter) { user, month, quarter ->
            Triple(user, month, quarter)
        }.flatMapLatest { (user, month, quarter) ->
            if (user == null) {
                return@flatMapLatest kotlinx.coroutines.flow.flowOf(HoursBreakdown())
            }

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
                HoursBreakdown(volunteers)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HoursBreakdown(),
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

data class HoursBreakdown(
    val total: Int = 0,
    val general: Int = 0,
    val specificByArea: Map<SpecificArea, Int> = emptyMap(),
) {
    companion object {
        operator fun invoke(volunteers: List<Volunteer>): HoursBreakdown {
            val shifts = volunteers.flatMap { it.shifts }
            return HoursBreakdown(
                total = shifts.sumOf { it.getHour() },
                general = shifts
                    .filter { it.type is VolunteerType.General }
                    .sumOf { it.getHour() },
                specificByArea = shifts
                    .mapNotNull { shift ->
                        (shift.type as? VolunteerType.Specific)
                            ?.let { it.specificArea to shift.getHour() }
                    }.groupBy({ it.first }, { it.second })
                    .mapValues { (_, hours) -> hours.sum() },
            )
        }
    }
}

private const val MINUTES_PER_HOUR = 60

data class Quarter(
    val number: Int,
    val year: Int,
) {
    fun startDate() = LocalDate(year, (number - 1) * 3 + 1, 1)

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