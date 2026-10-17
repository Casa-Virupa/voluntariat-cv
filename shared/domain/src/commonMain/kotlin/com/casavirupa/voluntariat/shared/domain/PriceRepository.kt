package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.payment.PriceRules
import com.casavirupa.voluntariat.shared.model.payment.PricingRules
import kotlinx.coroutines.flow.Flow

interface PriceRepository {
    // Generic timeline (`price_rules`), the fallback when the full tables can't be read
    fun getPriceRules(): Flow<PriceRules>

    // The dashboard's full tables (`item_price_rules` + `allowance_rules`); null = could not
    // read them or nothing published yet → fall back to getPriceRules()
    fun getPricingRules(): Flow<PricingRules?>
}
