package com.casavirupa.voluntariat.shared.model.user

import kotlinx.datetime.LocalDate
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
    // Long-stay volunteers only: the period they live at the house (set by the dashboard)
    val stayStart: LocalDate? = null,
    val stayEnd: LocalDate? = null,
    // Food handler certificate ("carnet de manipulador d'aliments"); the volunteer sets it
    // themselves from the profile screen. Defaults to false for users who never touched it.
    val hasFoodHandlerCertificate: Boolean = false,
) {
    val isHabitual: Boolean = volunteerType == UserVolunteerType.Habitual

    val isMitra: Boolean = volunteerType == UserVolunteerType.Mitra

    val isLongStay: Boolean = volunteerType == UserVolunteerType.LongStay

    // Long-stay volunteers live at the house: meals and nights are never charged and
    // they can't book them, so the app shows them no payment information at all.
    val paysForServices: Boolean = !isLongStay

    // Fallback used only when commitment_rules can't be read; long-stay targets are
    // always dashboard-defined, so there is no hardcoded default for them.
    fun getMonthHours(): Int =
        when (volunteerType) {
            UserVolunteerType.Mitra -> MITRA_HOURS
            UserVolunteerType.Habitual -> HABITUAL_HOURS
            UserVolunteerType.LongStay, null -> 0
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
    LongStay,
}
