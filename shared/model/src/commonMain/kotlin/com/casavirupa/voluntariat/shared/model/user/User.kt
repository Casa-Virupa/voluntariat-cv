package com.casavirupa.voluntariat.shared.model.user

import kotlin.jvm.JvmInline

data class User(
    val id: UserId,
    val name: String,
    val email: String,
    val role: UserRole,
    // Kept independently of role, mirroring the dashboard: an admin/coordinator can
    // still be a mitra and commitments/allowances are keyed on the raw volunteer_type
    val volunteerType: UserVolunteerType?,
    val hasOnboardingCompleted: Boolean,
    val specificAreas: List<SpecificArea>,
    val isMember: Boolean,
) {
    val isMitra: Boolean = volunteerType == UserVolunteerType.Mitra

    fun getMonthHours(): Int =
        when (volunteerType) {
            UserVolunteerType.Mitra -> MITRA_HOURS
            UserVolunteerType.Habitual -> HABITUAL_HOURS
            null -> 0
        }

    companion object {
        private const val MITRA_HOURS = 60
        private const val HABITUAL_HOURS = 8
    }
}

@JvmInline
value class UserId(val value: String) {
    companion object {
        val Empty = UserId("")
    }
}

sealed class UserRole {
    data class Volunteer(val type: UserVolunteerType) : UserRole()

    data object AreaResponsible : UserRole()

    data object CoordinationTeam : UserRole()

    data object Unknown : UserRole()
}

enum class UserVolunteerType {
    Habitual,
    Mitra,
}
