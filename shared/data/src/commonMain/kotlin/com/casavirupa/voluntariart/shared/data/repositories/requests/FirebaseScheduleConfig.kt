package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.Serializable

/** `configuration/schedule`: the house's volunteering hours the app warns about. */
@Serializable
data class FirebaseScheduleConfig(
    // "HH:mm", Casa Virupa time: when on-site volunteering ends on Sundays
    val sunday_end_time: String? = null,
    val updated_at: String? = null,
)
