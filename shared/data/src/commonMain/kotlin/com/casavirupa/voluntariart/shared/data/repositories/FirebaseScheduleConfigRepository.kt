package com.casavirupa.voluntariart.shared.data.repositories

import co.touchlab.kermit.Logger
import com.casavirupa.voluntariart.shared.data.repositories.requests.FirebaseScheduleConfig
import com.casavirupa.voluntariat.shared.domain.ScheduleConfigRepository
import com.casavirupa.voluntariat.shared.model.configuration.ScheduleConfig
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime

class FirebaseScheduleConfigRepository(
    private val firestore: FirebaseFirestore,
) : ScheduleConfigRepository {
    override fun getScheduleConfig(): Flow<ScheduleConfig> =
        firestore
            .collection("configuration")
            .document("schedule")
            .snapshots
            .map { snapshot ->
                if (snapshot.exists) {
                    snapshot.data<FirebaseScheduleConfig>().toDomainModel()
                } else {
                    ScheduleConfig.Default
                }
            }.catch { error ->
                // Typically PERMISSION_DENIED (console rules don't cover `configuration/schedule`)
                Logger.e(error, LOG_TAG) { "Error reading configuration/schedule, using the default hours" }
                emit(ScheduleConfig.Default)
            }
}

private const val LOG_TAG = "FirebaseScheduleConfigRepository"

// A missing or malformed time keeps the default rather than failing the doc.
private fun FirebaseScheduleConfig.toDomainModel() =
    ScheduleConfig(
        sundayEndTime = sunday_end_time
            ?.let { runCatching { LocalTime.parse(it.trim()) }.getOrNull() }
            ?: ScheduleConfig.DefaultSundayEndTime,
    )
