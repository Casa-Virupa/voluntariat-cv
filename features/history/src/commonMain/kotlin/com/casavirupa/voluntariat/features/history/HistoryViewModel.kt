package com.casavirupa.voluntariat.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.formatString
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.PaymentRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.payment.PaymentId
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
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.time.Clock

class HistoryViewModel(
    volunteerRepository: VolunteerRepository,
    authRepository: AuthRepository,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {
    private val _currentDate = MutableStateFlow(Clock.System.now().toDate())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val volunteersUiState =
        combine(
            currentDate,
            authRepository.getCurrentUserFlow(),
        ) { date, user ->
            date to user
        }.flatMapLatest { (date, user) ->
            volunteerRepository.getVolunteersByUserAndMonth(
                id = user.id,
                monthNumber = date.month.number,
                year = date.year
            )
        }.map { volunteers ->
            volunteers.toUiModel()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val paymentUiState: StateFlow<PaymentUiState> =
        combine(currentDate, authRepository.getCurrentUserFlow()) { date, user ->
            date to user
        }.flatMapLatest { (date, user) ->
            paymentRepository.getPaymentByYearMonth(user.id, date.toYearMonth())
        }.map { payment ->
            when {
                payment == null -> PaymentUiState.NotFound
                payment.paid -> PaymentUiState.Paid
                else -> PaymentUiState.NotPaid(payment.id, payment.amount)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = PaymentUiState.NotFound,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> =
        combine(
            volunteersUiState,
            paymentUiState,
        ) { volunteers, paymentState ->
            HistoryUiState(
                summary = MonthSummary(volunteers),
                volunteers = volunteers,
                paymentUiState = paymentState,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HistoryUiState.Empty,
        )

    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()

    fun nextMonth() {
        _currentDate.update { it.plus(DatePeriod(months = 1)) }
    }

    fun previousMonth() {
        _currentDate.update { it.minus(DatePeriod(months = 1)) }
    }

    fun showPaymentDialog() {
        _showPaymentDialog.update { true }
    }

    fun closePaymentDialog() {
        _showPaymentDialog.update { false }
    }

    fun confirmPayment() {
        viewModelScope.launch {
            val paymentState = paymentUiState.value
            if (paymentState !is PaymentUiState.NotPaid) {
                return@launch
            }
            paymentRepository.pay(
                id = paymentState.id,
                yearMonth = currentDate.value.toYearMonth(),
            )
            closePaymentDialog()
        }
    }

    private fun List<Volunteer>.toUiModel() =
        map {
            VolunteerHistoryItem(
                date = it.date,
                hours = it.shifts.sumOf { it.getTotalHour() }.toInt(),
                shifts = it.shifts,
                meals = it.meals,
            )
        }
}

data class HistoryUiState(
    val summary: MonthSummary,
    val volunteers: List<VolunteerHistoryItem>,
    val paymentUiState: PaymentUiState,
) {
    companion object {
        val Empty = HistoryUiState(
            summary = MonthSummary(DetailedSummary.Empty, DetailedSummary.Empty),
            volunteers = emptyList(),
            paymentUiState = PaymentUiState.NotFound,
        )
    }
}

data class MonthSummary(
    val days: DetailedSummary,
    val hours: DetailedSummary,
) {
    companion object {
        operator fun invoke(volunteers: List<VolunteerHistoryItem>): MonthSummary =
            MonthSummary(
                days = DetailedSummary.days(volunteers),
                hours = DetailedSummary.hours(volunteers),
            )
    }
}

data class DetailedSummary(
    val total: Number,
    val general: String,
    val specific: String,
) {
    companion object {
        val Empty = DetailedSummary(0, "0", "")

        fun days(volunteers: List<VolunteerHistoryItem>) =
            DetailedSummary(
                total = volunteers
                    .flatMap { it.shifts }
                    .sumOf { it.getTotalHour() }
                    .div(ALL_DAY_DIVIDER),
                general = volunteers
                    .flatMap { it.shifts }
                    .filter { it.type is VolunteerType.General }
                    .sumOf { it.getHoursFromVolunteerType(it.type) }
                    .div(ALL_DAY_DIVIDER)
                    .let { "${it.formatString(1)}d" },
                specific = volunteers
                    .flatMap { it.shifts }
                    .filter { it.type is VolunteerType.Specific }
                    .sumOf { it.getHoursFromVolunteerType(it.type) }
                    .div(ALL_DAY_DIVIDER)
                    .let {
                        if (it % ALL_DAY_DIVIDER == 0.0) {
                            "${it.toInt()}d"
                        } else {
                            "${it.formatString(1)}d"
                        }
                    },
            )

        fun hours(volunteers: List<VolunteerHistoryItem>) =
            DetailedSummary(
                total = volunteers
                    .flatMap { it.shifts }
                    .sumOf { it.getTotalHour() }
                    .toInt(),
                general = volunteers
                    .flatMap { it.shifts }
                    .filter { it.type is VolunteerType.General }
                    .sumOf { it.getHoursFromVolunteerType(it.type) }
                    .let { "${it.toInt()}h" },
                specific = volunteers
                    .flatMap { it.shifts }
                    .filter { it.type is VolunteerType.Specific }
                    .sumOf { it.getHoursFromVolunteerType(it.type) }
                    .let { "${it.toInt()}h" },
            )
    }
}

data class VolunteerHistoryItem(
    val date: LocalDate,
    val hours: Int,
    val shifts: List<Shift>,
    val meals: List<Meal>
)

sealed class PaymentUiState {
    data object Paid : PaymentUiState()
    data class NotPaid(
        val id: PaymentId,
        val amount: Double,
    ) : PaymentUiState()
    data object NotFound : PaymentUiState()
}

private fun Shift.getTotalHour() =
    when (this) {
        is Shift.Morning -> this.timeRange.start - this.timeRange.end
        is Shift.Afternoon -> this.timeRange.start - this.timeRange.end
    }

private fun Shift.getHoursFromVolunteerType(type: VolunteerType) =
    when (this) {
        is Shift.Morning -> {
            if (this.type == type) {
                this.timeRange.start - this.timeRange.end
            } else {
                0.0
            }
        }
        is Shift.Afternoon -> {
            if (this.type == type) {
                this.timeRange.start - this.timeRange.end
            } else {
                0.0
            }
        }
    }

private operator fun LocalTime.minus(other: LocalTime): Double {
    val diffSeconds = this.toSecondOfDay() - other.toSecondOfDay()
    return abs(diffSeconds) / 3600.0
}

private fun LocalDate.toYearMonth() =
    YearMonth(
        year = this.year,
        month = this.month,
    )


private const val ALL_DAY_DIVIDER = 8.0