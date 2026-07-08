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
) {
    val isMitra: Boolean =
        when (role) {
            is UserRole.Volunteer -> role.type == UserVolunteerType.Mitra
            else -> false
        }

    fun getMonthHours(): Int =
        when (this.role) {
            is UserRole.Volunteer -> {
                if (role.type == UserVolunteerType.Mitra) {
                    MITRA_HOURS
                } else {
                    HABITUAL_HOURS
                }
            }
            else -> 0
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
