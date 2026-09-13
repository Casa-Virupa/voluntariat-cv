package com.casavirupa.voluntariat.features.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.LedgerRepository
import com.casavirupa.voluntariat.shared.domain.PriceRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import com.casavirupa.voluntariat.shared.model.calendar.Meal
import com.casavirupa.voluntariat.shared.model.calendar.Shift
import com.casavirupa.voluntariat.shared.model.calendar.Volunteer
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerId
import com.casavirupa.voluntariat.shared.model.calendar.VolunteerType
import com.casavirupa.voluntariat.shared.model.payment.PriceRules
import com.casavirupa.voluntariat.shared.model.payment.calculateMonthlyCharge
import com.casavirupa.voluntariat.shared.model.payment.paymentUrl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlin.math.abs
import kotlin.time.Clock

class HistoryViewModel(
    private val volunteerRepository: VolunteerRepository,
    authRepository: AuthRepository,
    priceRepository: PriceRepository,
    ledgerRepository: LedgerRepository,
) : ViewModel() {
    private val _currentDate = MutableStateFlow(Clock.System.now().toDate())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    private val user = authRepository.getCurrentUserFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthData: StateFlow<MonthData?> =
        combine(
            currentDate,
            user,
        ) { date, user ->
            date to user
        }.flatMapLatest { (date, user) ->
            combine(
                volunteerRepository.getVolunteersByUserAndMonth(
                    id = user.id,
                    monthNumber = date.month.number,
                    year = date.year
                ),
                priceRepository.getPriceRules(),
            ) { volunteers, priceRules ->
                MonthData(
                    volunteers = volunteers,
                    priceRules = priceRules,
                    isMitra = user.isMitra,
                    paysForServices = user.paysForServices,
                    userName = user.name,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // Global outstanding balance: charges over ALL the user's bookings (past and
    // future) minus everything the admin has recorded in the ledger. Charges are
    // netted per calendar month so the mitra allowance never carries over. Null
    // until all three sources have emitted, so the UI never shows a wrong amount.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val pendingBalance: StateFlow<Double?> =
        user.flatMapLatest { user ->
            if (!user.paysForServices) return@flatMapLatest flowOf(0.0)
            combine(
                volunteerRepository.getVolunteersByUserFlow(user.id),
                priceRepository.getPriceRules(),
                ledgerRepository.getEntriesByUser(user.id),
            ) { allVolunteers, priceRules, ledger ->
                allVolunteers
                    .groupBy { it.date.year to it.date.month }
                    .values
                    .sumOf { monthVolunteers ->
                        calculateMonthlyCharge(monthVolunteers, priceRules, user.isMitra).total
                    } - ledger.sumOf { it.amount }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    val uiState: StateFlow<HistoryUiState> =
        combine(
            monthData.filterNotNull(),
            pendingBalance,
        ) { data, balance ->
            val volunteers = data.volunteers.toUiModel()
            // Only a positive balance is payable; the web reads the amount from the URL.
            val pendingBalance = balance?.takeIf { it > AMOUNT_TOLERANCE }
            val paymentDetail = if (data.paysForServices) {
                PaymentDetail(
                    volunteers = data.volunteers,
                    priceRules = data.priceRules,
                    isMitra = data.isMitra,
                    balance = balance ?: 0.0,
                    paymentUrl = pendingBalance?.let { paymentUrl(data.userName, it) },
                )
            } else {
                PaymentDetail.Empty
            }
            HistoryUiState(
                summary = DetailedSummary.hours(volunteers),
                volunteers = volunteers,
                showNoVolunteeringMessage = data.volunteers.isEmpty(),
                paymentUiState = if (pendingBalance != null) {
                    PaymentUiState.Pending(
                        monthAmount = paymentDetail.total,
                        balance = pendingBalance,
                    )
                } else {
                    PaymentUiState.Hidden
                },
                paymentDetail = paymentDetail,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HistoryUiState.Empty,
        )

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    private var selectedVolunteerToDelete: VolunteerId? = null

    fun onDeleteVolunteer(id: VolunteerId) {
        selectedVolunteerToDelete = id
        _showDeleteDialog.update { true }
    }

    fun onCloseDeleteDialog() {
        selectedVolunteerToDelete = null
        _showDeleteDialog.update { false }
    }

    fun deleteVolunteer() {
        val volunteerId = selectedVolunteerToDelete ?: return
        viewModelScope.launch {
            volunteerRepository
                .deleteVolunteer(volunteerId)
                .onSuccess {
                    onCloseDeleteDialog()
                }.onFailure {
                    // TODO: Handle the failure
                }
        }
    }

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

    private fun List<Volunteer>.toUiModel(): List<VolunteerHistoryItem> {
        val today = Clock.System.now().toDate()
        return map {
            VolunteerHistoryItem(
                id = it.id,
                date = it.date,
                hours = it.shifts.sumOf { it.getTotalHour() }.toInt(),
                shifts = it.shifts,
                meals = it.meals,
                sleep = it.sleep,
                canBeDeleted = it.date > today,
            )
        }
    }
}

data class HistoryUiState(
    val summary: DetailedSummary,
    val volunteers: List<VolunteerHistoryItem>,
    val showNoVolunteeringMessage: Boolean,
    val paymentUiState: PaymentUiState,
    val paymentDetail: PaymentDetail,
) {
    companion object {
        val Empty = HistoryUiState(
            summary = DetailedSummary.Empty,
            volunteers = emptyList(),
            showNoVolunteeringMessage = false,
            paymentUiState = PaymentUiState.Hidden,
            paymentDetail = PaymentDetail.Empty,
        )
    }
}

private data class MonthData(
    val volunteers: List<Volunteer>,
    val priceRules: PriceRules,
    val isMitra: Boolean,
    val paysForServices: Boolean,
    val userName: String,
)

data class PaymentDetail(
    val lunches: Int,
    val dinners: Int,
    val nights: Int,
    val breakfasts: Int,
    val breakfastsNextDay: Int,
    val lunchesAmount: Double,
    val dinnersAmount: Double,
    val nightsAmount: Double,
    val breakfastsAmount: Double,
    val breakfastsNextDayAmount: Double,
    val freeLunches: Int,
    val freeDinners: Int,
    val freeNights: Int,
    val lunchesDiscount: Double,
    val dinnersDiscount: Double,
    val nightsDiscount: Double,
    // Net charge of the month being viewed
    val total: Double,
    // Outstanding balance over all months (charges minus ledger); what the volunteer pays
    val balance: Double,
    // Pre-filled link to the payment page; null when there is nothing to pay
    val paymentUrl: String?,
) {
    companion object {
        val Empty = PaymentDetail(
            lunches = 0,
            dinners = 0,
            nights = 0,
            breakfasts = 0,
            breakfastsNextDay = 0,
            lunchesAmount = 0.0,
            dinnersAmount = 0.0,
            nightsAmount = 0.0,
            breakfastsAmount = 0.0,
            breakfastsNextDayAmount = 0.0,
            freeLunches = 0,
            freeDinners = 0,
            freeNights = 0,
            lunchesDiscount = 0.0,
            dinnersDiscount = 0.0,
            nightsDiscount = 0.0,
            total = 0.0,
            balance = 0.0,
            paymentUrl = null,
        )

        operator fun invoke(
            volunteers: List<Volunteer>,
            priceRules: PriceRules,
            isMitra: Boolean,
            balance: Double,
            paymentUrl: String?,
        ): PaymentDetail {
            val breakdown = calculateMonthlyCharge(volunteers, priceRules, isMitra)
            return PaymentDetail(
                lunches = breakdown.lunches,
                dinners = breakdown.dinners,
                nights = breakdown.nights,
                breakfasts = breakdown.breakfasts,
                breakfastsNextDay = breakdown.breakfastsNextDay,
                lunchesAmount = breakdown.lunchesAmount,
                dinnersAmount = breakdown.dinnersAmount,
                nightsAmount = breakdown.nightsAmount,
                breakfastsAmount = breakdown.breakfastsAmount,
                breakfastsNextDayAmount = breakdown.breakfastsNextDayAmount,
                freeLunches = breakdown.freeLunchesUsed,
                freeDinners = breakdown.freeDinnersUsed,
                freeNights = breakdown.freeNightsUsed,
                lunchesDiscount = breakdown.lunchesDiscount,
                dinnersDiscount = breakdown.dinnersDiscount,
                nightsDiscount = breakdown.nightsDiscount,
                total = breakdown.total,
                balance = balance,
                paymentUrl = paymentUrl,
            )
        }
    }
}

data class DetailedSummary(
    val total: Int,
    val general: String,
    val specific: String,
) {
    companion object {
        val Empty = DetailedSummary(0, "0", "")

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
    val id: VolunteerId,
    val date: LocalDate,
    val hours: Int,
    val shifts: List<Shift>,
    val meals: List<Meal>,
    val sleep: Boolean,
    val canBeDeleted: Boolean = false,
)

sealed class PaymentUiState {
    data object Hidden : PaymentUiState()
    // [monthAmount] is the net charge of the month being viewed; [balance] the global
    // outstanding amount the volunteer is asked to pay.
    data class Pending(
        val monthAmount: Double,
        val balance: Double,
    ) : PaymentUiState()
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

private const val AMOUNT_TOLERANCE = 0.001