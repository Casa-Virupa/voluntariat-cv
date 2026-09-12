package com.casavirupa.voluntariat.shared.model.payment

import kotlinx.datetime.LocalDate

data class PriceRule(
    val validFrom: LocalDate,
    val prices: Prices,
)

data class PriceRules(val rules: List<PriceRule>) {
    private val sortedRules = rules.sortedBy { it.validFrom }

    fun priceAt(date: LocalDate): Prices =
        sortedRules.lastOrNull { it.validFrom <= date }?.prices ?: Prices.Default

    companion object {
        val Empty = PriceRules(emptyList())
    }
}
