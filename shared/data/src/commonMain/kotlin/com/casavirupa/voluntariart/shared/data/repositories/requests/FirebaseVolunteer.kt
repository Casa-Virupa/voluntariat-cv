package com.casavirupa.voluntariart.shared.data.repositories.requests

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseVolunteer(
    val userId: String,
    val timestamp: Timestamp,
    val shift: String,
    val specificArea: String?,
    val mealTypes: List<String>,
    val sleep: Boolean,
)
