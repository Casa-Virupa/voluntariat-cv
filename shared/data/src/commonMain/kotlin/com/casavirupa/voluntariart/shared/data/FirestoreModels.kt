package com.casavirupa.voluntariart.shared.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirestoreUser(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    @SerialName("volunteer_type") val volunteerType: String?,
    @SerialName("onboarding_completed") val hasOnboardingCompleted: Boolean,
    @SerialName("specific_areas") val specificAreas: List<String>,
    @SerialName("is_member") val isMember: Boolean,
    // ISO yyyy-MM-dd, long-stay volunteers only
    @SerialName("stay_start") val stayStart: String? = null,
    @SerialName("stay_end") val stayEnd: String? = null,
)
