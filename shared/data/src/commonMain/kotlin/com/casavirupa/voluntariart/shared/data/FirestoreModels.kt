package com.casavirupa.voluntariart.shared.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirestoreUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("onboarding_completed") val hasOnboardingCompleted: Boolean,
)
