package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebasePriceRule
import com.casavirupa.voluntariat.shared.domain.PriceRepository
import com.casavirupa.voluntariat.shared.model.payment.PriceRule
import com.casavirupa.voluntariat.shared.model.payment.PriceRules
import com.casavirupa.voluntariat.shared.model.payment.Prices
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FirebasePriceRepository(
    private val firestore: FirebaseFirestore,
) : PriceRepository {
    override fun getPriceRules(): Flow<PriceRules> =
        firestore
            .collection("price_rules")
            .snapshots
            .map { snapshot ->
                PriceRules(
                    snapshot.documents.mapNotNull { doc ->
                        runCatching { doc.data<FirebasePriceRule>() }
                            .getOrNull()
                            ?.toDomainModelOrNull()
                    },
                )
            }.catch { emit(PriceRules.Empty) }
}

private fun FirebasePriceRule.toDomainModelOrNull(): PriceRule? =
    runCatching { LocalDate.parse(validFrom) }
        .getOrNull()
        ?.let { date ->
            PriceRule(
                validFrom = date,
                prices = Prices(
                    lunch = lunch,
                    dinner = dinner,
                    breakfast = breakfast,
                    sleep = sleep,
                ),
            )
        }
