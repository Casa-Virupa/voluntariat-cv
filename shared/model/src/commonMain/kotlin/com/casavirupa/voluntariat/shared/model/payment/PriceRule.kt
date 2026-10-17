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

    // For a volunteer the monthly allowance doesn't apply to
    fun withoutAllowance(): PriceRules =
        PriceRules(rules.map { it.copy(prices = it.prices.copy(mitraAllowance = MitraAllowance.None)) })

    companion object {
        val Empty = PriceRules(emptyList())
    }
}
