package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseInterestLinks
import com.casavirupa.voluntariat.shared.core.utils.toDate
import com.casavirupa.voluntariat.shared.domain.InterestLinksRepository
import com.casavirupa.voluntariat.shared.model.configuration.InterestLinks
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class FirebaseInterestLinksRepository(
    private val firestore: FirebaseFirestore,
) : InterestLinksRepository {
    override fun getInterestLinks(): Flow<InterestLinks> =
        firestore
            .collection("configuration")
            .document("links")
            .snapshots
            .map { snapshot ->
                if (snapshot.exists) {
                    snapshot.data<FirebaseInterestLinks>().toDomainModel()
                } else {
                    InterestLinks.Empty
                }
            }.catch { emit(InterestLinks.Empty) }
}

private fun FirebaseInterestLinks.toDomainModel() =
    InterestLinks(
        text = text,
        updatedAt = updated_at
            ?.let { runCatching { Instant.parse(it).toDate() }.getOrNull() },
    )
