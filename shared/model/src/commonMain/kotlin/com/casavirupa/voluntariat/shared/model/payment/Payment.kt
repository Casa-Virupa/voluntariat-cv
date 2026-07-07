package com.casavirupa.voluntariat.shared.model.payment

import kotlinx.datetime.YearMonth
import kotlin.jvm.JvmInline

data class Payment(
    val id: PaymentId,
    val yearMonth: YearMonth,
    val paid: Boolean,
)

@JvmInline
value class PaymentId(val value: String)
