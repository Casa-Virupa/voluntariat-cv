package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseLedgerEntry
import com.casavirupa.voluntariat.shared.domain.LedgerRepository
import com.casavirupa.voluntariat.shared.model.payment.LedgerEntry
import com.casavirupa.voluntariat.shared.model.payment.LedgerEntryKind
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FirebaseLedgerRepository(
    private val firestore: FirebaseFirestore,
) : LedgerRepository {
    override fun getEntriesByUser(id: UserId): Flow<List<LedgerEntry>> =
        firestore
            .collection("ledger")
            .where { "user_id" equalTo id.value }
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.data<FirebaseLedgerEntry>() }
                        .getOrNull()
                        ?.toDomainModelOrNull()
                }
            }.catch { emit(emptyList()) }
}

private fun FirebaseLedgerEntry.toDomainModelOrNull(): LedgerEntry? {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return LedgerEntry(
        userId = UserId(userId),
        date = parsedDate,
        amount = amount,
        kind = when (kind) {
            "payment" -> LedgerEntryKind.Payment
            "adjustment" -> LedgerEntryKind.Adjustment
            else -> LedgerEntryKind.Unknown
        },
        note = note,
    )
}
