package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseInterestLinks(
    val text: String = "",
    val updated_at: String? = null,
)
