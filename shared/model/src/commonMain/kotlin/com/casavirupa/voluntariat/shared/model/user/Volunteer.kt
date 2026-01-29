package com.casavirupa.voluntariat.shared.model.user

import com.casavirupa.voluntariat.shared.model.area.AreaId

data class Volunteer(
    val userId: UserId,
    val commitmentGrade: CommitmentGrade,
    val type: VolunteerType,
    val areaId: AreaId,
)

enum class CommitmentGrade(val totalHours: Int) {
    Monthly(MONTHLY_TOTAL_HOURS),
    Quarterly(QUARTERLY_TOTAL_HOURS)
}

enum class VolunteerType {
    Usual,
    // TODO: Define more types of volunteers
}

private const val MONTHLY_TOTAL_HOURS = 8
private const val QUARTERLY_TOTAL_HOURS = 16
