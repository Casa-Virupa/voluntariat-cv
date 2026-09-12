package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.payment.LedgerEntry
import com.casavirupa.voluntariat.shared.model.user.UserId
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun getEntriesByUser(id: UserId): Flow<List<LedgerEntry>>
}
