package com.casavirupa.voluntariart.shared.data.repositories

import co.touchlab.kermit.Logger
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseAreaConfig
import com.casavirupa.voluntariat.shared.domain.AreaConfigRepository
import com.casavirupa.voluntariat.shared.model.configuration.AreaConfig
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class FirebaseAreaConfigRepository(
    private val firestore: FirebaseFirestore,
) : AreaConfigRepository {
    override fun getAreaConfig(): Flow<AreaConfig> =
        firestore
            .collection("configuration")
            .document("areas")
            .snapshots
            .map { snapshot ->
                if (snapshot.exists) {
                    snapshot.data<FirebaseAreaConfig>().toDomainModel()
                } else {
                    AreaConfig.Empty
                }
            }.catch { error ->
                // Typically a Firestore PERMISSION_DENIED (console rules don't cover
                // `configuration/areas`) or a malformed doc. Log it: otherwise the silent
                // fallback to Empty looks like "no area allows online".
                Logger.e(error, LOG_TAG) { "Error reading configuration/areas, no area is online" }
                emit(AreaConfig.Empty)
            }
}

private const val LOG_TAG = "FirebaseAreaConfigRepository"

// Unknown codes (an area the app doesn't know yet) are dropped rather than failing the doc.
private fun FirebaseAreaConfig.toDomainModel() =
    AreaConfig(
        onlineAreas = online_areas
            .map { it.toSpecificArea() }
            .filter { it != SpecificArea.Unknown }
            .toSet(),
    )
