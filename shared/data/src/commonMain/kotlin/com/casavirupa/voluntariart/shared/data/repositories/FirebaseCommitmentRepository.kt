package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseCommitmentRule
import com.casavirupa.voluntariat.shared.domain.CommitmentRepository
import com.casavirupa.voluntariat.shared.model.commitment.CommitmentPeriod
import com.casavirupa.voluntariat.shared.model.commitment.CommitmentRule
import com.casavirupa.voluntariat.shared.model.commitment.CommitmentRules
import com.casavirupa.voluntariat.shared.model.commitment.CommitmentScope
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FirebaseCommitmentRepository(
    private val firestore: FirebaseFirestore,
) : CommitmentRepository {
    override fun getCommitmentRules(): Flow<CommitmentRules?> =
        firestore
            .collection("commitment_rules")
            .snapshots
            .map { snapshot ->
                val rules: CommitmentRules? = CommitmentRules(
                    snapshot.documents.mapNotNull { doc ->
                        runCatching { doc.data<FirebaseCommitmentRule>() }
                            .getOrNull()
                            ?.toDomainModelOrNull(doc.id)
                    },
                )
                rules
            }.catch { emit(null) }
}

private fun FirebaseCommitmentRule.toDomainModelOrNull(id: String): CommitmentRule? {
    val scope = when (scopeKind) {
        "global" -> CommitmentScope.Global
        "volunteer_type" -> scopeValue.toVolunteerType()?.let { CommitmentScope.VolunteerType(it) }
        "user" -> scopeValue?.let { CommitmentScope.User(UserId(it)) }
        else -> null
    } ?: return null
    val period = when (periodKind) {
        "month" -> CommitmentPeriod.Month
        "quarter" -> CommitmentPeriod.Quarter
        else -> null
    } ?: return null
    if (area.isBlank() || targetMinutes < 0) return null
    val from = runCatching { LocalDate.parse(validFrom) }.getOrNull() ?: return null
    val to = validTo?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return null }
    return CommitmentRule(
        id = id,
        scope = scope,
        area = area,
        periodKind = period,
        targetMinutes = targetMinutes,
        validFrom = from,
        validTo = to,
    )
}
