package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseCommitmentRule(
    @SerialName("scope_kind") val scopeKind: String = "",
    @SerialName("scope_value") val scopeValue: String? = null,
    val area: String = "",
    @SerialName("period_kind") val periodKind: String = "",
    @SerialName("target_minutes") val targetMinutes: Int = -1,
    @SerialName("valid_from") val validFrom: String = "",
    @SerialName("valid_to") val validTo: String? = null,
)
