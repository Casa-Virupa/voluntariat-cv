package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseVolunteer(
    val userId: String,
    val date: String,
    val shift: String,
    val specificArea: String?,
    val mealTypes: List<String>,
    val sleep: Boolean,
)
