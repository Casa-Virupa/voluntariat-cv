package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.payment.Prices
import kotlinx.coroutines.flow.Flow

interface PriceRepository {
    fun getPrices(): Flow<Prices>

    suspend fun getCurrentPrices(): Prices
}
