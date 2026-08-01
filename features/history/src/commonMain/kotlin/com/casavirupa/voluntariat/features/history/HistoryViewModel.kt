package com.casavirupa.voluntariat.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.formatString
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.PaymentRepository
import com.casavirupa.voluntariat.shared.domain.PriceRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.payment.Payment
import com.casavirupa.voluntariat.shared.model.payment.PaymentId
import com.casavirupa.voluntariat.shared.model.payment.Prices
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
    priceRepository: PriceRepository,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {
    private val _currentDate = MutableStateFlow(Clock.System.now().toDate())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthData: StateFlow<MonthData?> =
        combine(
            currentDate,
            authRepository.getCurrentUserFlow(),
        ) { date, user ->
            date to user
        }.flatMapLatest { (date, user) ->
            combine(
                volunteerRepository.getVolunteersByUserAndMonth(
                    id = user.id,
                    monthNumber = date.month.number,
                    year = date.year
                ),
                paymentRepository.getPaymentByYearMonth(user.id, date.toYearMonth()),
                priceRepository.getPrices(),
            ) { volunteers, payment, prices ->
                MonthData(volunteers, payment, prices)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    val uiState: StateFlow<HistoryUiState> =
        monthData
            .filterNotNull()
            .map { data ->
                val volunteers = data.volunteers.toUiModel()
                HistoryUiState(
                    summary = MonthSummary(volunteers),
                    volunteers = volunteers,
                    paymentUiState = when {
                        data.payment == null -> PaymentUiState.NotFound
                        data.payment.paid -> PaymentUiState.Paid
                        else -> PaymentUiState.NotPaid(data.payment.id, data.payment.amount)
                    },
                    paymentDetail = PaymentDetail(volunteers, data.prices),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = HistoryUiState.Empty,
            )

    init {
        // Keeps pending payments coherent with the configured prices: whenever the
        // stored amount of a "paid = false" payment no longer matches the month's
        // bookings priced at the current rates, the amount is rewritten in Firestore.
        viewModelScope.launch {
            monthData.filterNotNull().collect { data ->
                val payment = data.payment ?: return@collect
                if (payment.paid) return@collect
                val expected = data.volunteers.sumOf { it.calculateTotalToPay(data.prices) }
                if (abs(expected - payment.amount) > AMOUNT_TOLERANCE) {
                    paymentRepository.updateAmount(payment.id, expected)
                }
            }
        }
    }

    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()

    private val _showPaymentDetailDialog = MutableStateFlow(false)
    val showPaymentDetailDialog: StateFlow<Boolean> = _showPaymentDetailDialog.asStateFlow()

    fun showPaymentDetailDialog() {
        _showPaymentDetailDialog.update { true }
    }

    fun closePaymentDetailDialog() {
        _showPaymentDetailDialog.update { false }
    }

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
            val paymentState = uiState.value.paymentUiState
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
                sleep = it.sleep,
            )
        }
}

data class HistoryUiState(
    val summary: MonthSummary,
    val volunteers: List<VolunteerHistoryItem>,
    val paymentUiState: PaymentUiState,
    val paymentDetail: PaymentDetail,
) {
    companion object {
        val Empty = HistoryUiState(
            summary = MonthSummary(DetailedSummary.Empty, DetailedSummary.Empty),
            volunteers = emptyList(),
            paymentUiState = PaymentUiState.NotFound,
            paymentDetail = PaymentDetail.Empty,
        )
    }
}

private data class MonthData(
    val volunteers: List<Volunteer>,
    val payment: Payment?,
    val prices: Prices,
)

data class PaymentDetail(
    val lunches: Int,
    val dinners: Int,
    val nights: Int,
    val prices: Prices = Prices.Default,
) {
    val lunchesAmount: Double get() = lunches * prices.lunch
    val dinnersAmount: Double get() = dinners * prices.dinner
    val nightsAmount: Double get() = nights * prices.sleep
    val total: Double get() = lunchesAmount + dinnersAmount + nightsAmount

    companion object {
        val Empty = PaymentDetail(lunches = 0, dinners = 0, nights = 0)

        operator fun invoke(volunteers: List<VolunteerHistoryItem>, prices: Prices) =
            PaymentDetail(
                lunches = volunteers.sumOf { item -> item.meals.count { it == Meal.Lunch } },
                dinners = volunteers.sumOf { item -> item.meals.count { it == Meal.Dinner } },
                nights = volunteers.count { it.sleep },
                prices = prices,
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
    val meals: List<Meal>,
    val sleep: Boolean,
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

private const val AMOUNT_TOLERANCE = 0.001