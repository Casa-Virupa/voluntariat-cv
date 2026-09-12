package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseInterestLinks(
    val text: String = "",
    val text_habitual: String = "",
    val text_mitra: String = "",
    val text_long_stay: String = "",
    val updated_at: String? = null,
)
