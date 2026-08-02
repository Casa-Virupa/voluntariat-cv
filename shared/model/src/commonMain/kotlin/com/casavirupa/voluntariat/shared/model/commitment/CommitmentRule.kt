package com.casavirupa.voluntariat.shared.model.commitment

import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

enum class CommitmentPeriod {
    Month,
    Quarter,
}

sealed class CommitmentScope {
    data object Global : CommitmentScope()

    data class VolunteerType(val type: UserVolunteerType) : CommitmentScope()

    data class User(val uid: UserId) : CommitmentScope()

    val specificity: Int
        get() = when (this) {
            is User -> 3
            is VolunteerType -> 2
            Global -> 1
        }
}

data class CommitmentRule(
    val id: String,
    val scope: CommitmentScope,
    val area: String,
    val periodKind: CommitmentPeriod,
    val targetMinutes: Int,
    val validFrom: LocalDate,
    // Exclusive; null = still in force
    val validTo: LocalDate?,
) {
    val numericId: Int = id.substringAfterLast('-').toIntOrNull() ?: -1

    fun appliesTo(uid: UserId, volunteerType: UserVolunteerType?): Boolean =
        when (val scope = scope) {
            CommitmentScope.Global -> true
            is CommitmentScope.VolunteerType -> volunteerType != null && volunteerType == scope.type
            is CommitmentScope.User -> scope.uid == uid
        }

    fun isValidAt(periodStart: LocalDate): Boolean {
        val to = validTo
        return validFrom <= periodStart && (to == null || periodStart < to)
    }
}

data class CommitmentTarget(
    // Already scaled to the viewed period
    val targetMinutes: Int,
    val nativePeriod: CommitmentPeriod,
    // A scaled target must never be reported as failed: the native period isn't over
    val isScaled: Boolean,
) {
    val targetHours: Double get() = targetMinutes / MINUTES_PER_HOUR

    companion object {
        private const val MINUTES_PER_HOUR = 60.0
    }
}

data class CommitmentRules(val rules: List<CommitmentRule>) {
    fun targetFor(
        uid: UserId,
        volunteerType: UserVolunteerType?,
        periodStart: LocalDate,
        viewedPeriod: CommitmentPeriod,
        area: String = TOTAL_AREA,
    ): CommitmentTarget? {
        val winner = rules
            .filter { it.area == area }
            .filter { it.appliesTo(uid, volunteerType) }
            .filter { it.isValidAt(periodStart) }
            .maxWithOrNull(
                compareBy({ it.scope.specificity }, { it.validFrom }, { it.numericId }),
            ) ?: return null

        val minutes = when {
            winner.periodKind == viewedPeriod -> winner.targetMinutes
            winner.periodKind == CommitmentPeriod.Quarter ->
                (winner.targetMinutes / MONTHS_PER_QUARTER).roundToInt()
            else -> winner.targetMinutes * MONTHS_PER_QUARTER.toInt()
        }
        return CommitmentTarget(
            targetMinutes = minutes,
            nativePeriod = winner.periodKind,
            isScaled = winner.periodKind != viewedPeriod,
        )
    }

    companion object {
        const val TOTAL_AREA = "__total__"
        private const val MONTHS_PER_QUARTER = 3.0
        val Empty = CommitmentRules(emptyList())
    }
}
