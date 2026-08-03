package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseLedgerEntry(
    @SerialName("user_id") val userId: String = "",
    val date: String = "",
    val amount: Double = 0.0,
    val kind: String = "payment",
    val note: String? = null,
)
