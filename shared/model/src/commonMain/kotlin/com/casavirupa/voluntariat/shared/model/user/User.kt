package com.casavirupa.voluntariat.shared.model.user

import kotlin.jvm.JvmInline

data class User(
    val id: UserId,
    val name: String,
    val email: String,
    val role: UserRole,
    val hasOnboardingCompleted: Boolean,
)

@JvmInline
value class UserId(val value: String) {
    companion object {
        val Empty = UserId("")
    }
}

enum class UserRole {
    Volunteer,
    AreaResponsible,
    CoordinationTeam,
    Unknown,
}
