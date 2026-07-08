package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebasePayment(
    @SerialName("user_id") val userId: String,
    val year: Int,
    val month: Int,
    val paid: Boolean,
    val amount: Double = 0.0,
)