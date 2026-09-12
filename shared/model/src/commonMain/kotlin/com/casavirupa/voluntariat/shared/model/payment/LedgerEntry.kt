package com.casavirupa.voluntariat.shared.model.payment

import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.datetime.LocalDate

data class LedgerEntry(
    val userId: UserId,
    val date: LocalDate,
    val amount: Double,
    val kind: LedgerEntryKind,
    val note: String?,
)

enum class LedgerEntryKind { Payment, Adjustment, Unknown }
