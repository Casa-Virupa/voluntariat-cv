package com.casavirupa.voluntariart.shared.data.repositories.requests

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseVolunteer(
    val userId: String,
    val timestamp: Timestamp,
    val shift: String,
    val types: List<String>,
    val timeRanges: List<FirebaseTimeRange>,
    val mealTypes: List<String>,
    val sleep: Boolean,
)

@Serializable
data class FirebaseTimeRange(
    val start: LocalTime,
    val end: LocalTime,
)