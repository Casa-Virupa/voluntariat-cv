package com.casavirupa.voluntariat.shared.model.user

import kotlin.jvm.JvmInline

data class User(
    val id: UserId,
    val name: String,
    val email: String,
    val role: UserRole,
    val hasOnboardingCompleted: Boolean,
    val specificAreas: List<SpecificArea>,
    val isMember: Boolean,
)

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
