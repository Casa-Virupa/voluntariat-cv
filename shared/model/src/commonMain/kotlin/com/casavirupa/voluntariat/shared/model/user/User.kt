package com.casavirupa.voluntariat.shared.model.user

import kotlin.jvm.JvmInline

data class User(
    val id: UserId,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val role: UserRole,
)

@JvmInline
value class UserId(val value: String)

enum class UserRole {
    Volunteer,
    AreaResponsible,
    CoordinationTeam,
}