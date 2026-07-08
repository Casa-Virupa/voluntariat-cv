package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.payment.Payment
import com.casavirupa.voluntariat.shared.model.payment.PaymentId
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.YearMonth

interface PaymentRepository {
    fun getPaymentByYearMonth(yearMonth: YearMonth): Flow<Payment?>

    suspend fun pay(id: PaymentId, yearMonth: YearMonth): Result<Unit>

    suspend fun addPayment(
        userId: UserId,
        yearMonth: YearMonth,
        amount: Double,
    ): Result<Unit>
}