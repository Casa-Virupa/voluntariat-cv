package com.casavirupa.voluntariart.shared.data.repositories.requests

import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseVolunteer(
    @SerialName("user_id") val userId: String,
    val timestamp: Timestamp,
    val shifts: List<FirebaseShift>,
    @SerialName("meal_types") val mealTypes: List<String>,
    val sleep: Boolean,
)

@Serializable
data class FirebaseShift(
    val shift: String,
    @SerialName("time_range") val timeRange: FirebaseTimeRange,
    val type: FirebaseVolunteerType,
    // Absent on bookings made before the online toggle existed → on-site
    val online: Boolean = false,
)

@Serializable
data class FirebaseVolunteerType(
    val type: String,
    @SerialName("specific_areas") val specificAreas: String? = null,
)

@Serializable
data class FirebaseTimeRange(
    val start: LocalTime,
    val end: LocalTime,
)