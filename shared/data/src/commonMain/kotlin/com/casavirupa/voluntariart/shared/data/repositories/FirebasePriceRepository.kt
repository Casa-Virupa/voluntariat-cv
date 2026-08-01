package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebasePrices
import com.casavirupa.voluntariat.shared.domain.PriceRepository
import com.casavirupa.voluntariat.shared.model.payment.Prices
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class FirebasePriceRepository(
    private val firestore: FirebaseFirestore,
) : PriceRepository {
    override fun getPrices(): Flow<Prices> =
        firestore
            .collection("configuration")
            .document("prices")
            .snapshots
            .map { snapshot ->
                if (snapshot.exists) {
                    snapshot.data<FirebasePrices>().toDomainModel()
                } else {
                    Prices.Default
                }
            }.catch { emit(Prices.Default) }

    override suspend fun getCurrentPrices(): Prices =
        runCatching {
            firestore
                .collection("configuration")
                .document("prices")
                .get()
                .takeIf { it.exists }
                ?.data<FirebasePrices>()
                ?.toDomainModel()
        }.getOrNull() ?: Prices.Default
}

private fun FirebasePrices.toDomainModel() =
    Prices(
        lunch = lunch,
        dinner = dinner,
        breakfast = breakfast,
        sleep = sleep,
    )
