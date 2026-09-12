package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.commitment.CommitmentRules
import kotlinx.coroutines.flow.Flow

interface CommitmentRepository {
    // Emits null when the collection could not be read at all,
    // signalling the caller to fall back to the hardcoded defaults.
    fun getCommitmentRules(): Flow<CommitmentRules?>
}
