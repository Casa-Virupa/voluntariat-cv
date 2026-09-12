package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.payment.PriceRules
import kotlinx.coroutines.flow.Flow

interface PriceRepository {
    fun getPriceRules(): Flow<PriceRules>
}
