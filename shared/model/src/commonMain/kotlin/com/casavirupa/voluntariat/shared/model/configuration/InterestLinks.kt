package com.casavirupa.voluntariat.shared.model.configuration

import kotlinx.datetime.LocalDate

data class InterestLinks(
    val text: String = "",
    val updatedAt: LocalDate? = null,
) {
    companion object {
        val Empty = InterestLinks()
    }
}
