package com.casavirupa.voluntariart.shared.data.repositories.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// `item_price_rules/dash-<n>`; contract in ../voluntariat-dashboard/PRICES-APP.md
@Serializable
data class FirebaseItemPriceRule(
    val item: String = "",
    // Absent = any
    @SerialName("volunteer_type") val volunteerType: String? = null,
    @SerialName("is_member") val isMember: Boolean? = null,
    @SerialName("with_volunteering") val withVolunteering: Boolean? = null,
    val price: Double = -1.0,
    @SerialName("valid_from") val validFrom: String = "",
    @SerialName("valid_to") val validTo: String? = null,
)

// `allowance_rules/dash-<n>`
@Serializable
data class FirebaseAllowanceRule(
    @SerialName("volunteer_type") val volunteerType: String = "",
    @SerialName("free_meals") val freeMeals: Int = -1,
    @SerialName("free_sleeps") val freeSleeps: Int = -1,
    @SerialName("valid_from") val validFrom: String = "",
    @SerialName("valid_to") val validTo: String? = null,
)
