package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.Serializable

/** `configuration/areas`, written by the dashboard: area codes whose volunteering may be online. */
@Serializable
data class FirebaseAreaConfig(
    val online_areas: List<String> = emptyList(),
    val updated_at: String? = null,
)
