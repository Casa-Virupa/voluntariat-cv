package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseVolunteer(
    val userId: String,
    val date: String,
    val scheduleRange: String,
    val technicalAreaTurn: String,
    val mealType: String,
    val sleep: Boolean,
)
