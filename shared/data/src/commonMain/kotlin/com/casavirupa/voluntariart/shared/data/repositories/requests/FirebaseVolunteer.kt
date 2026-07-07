package com.casavirupa.voluntariart.shared.data.repositories.requests

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseVolunteer(
    val userId: String,
    val timestamp: Timestamp,
    val shifts: List<FirebaseShift>,
    val mealTypes: List<String>,
    val sleep: Boolean,
)

@Serializable
data class FirebaseShift(
    val shift: String,
    val timeRange: FirebaseTimeRange,
    val type: FirebaseVolunteerType,
)

@Serializable
data class FirebaseVolunteerType(
    val type: String,
    val specificAreas: String? = null,
)

@Serializable
data class FirebaseTimeRange(
    val start: LocalTime,
    val end: LocalTime,
)