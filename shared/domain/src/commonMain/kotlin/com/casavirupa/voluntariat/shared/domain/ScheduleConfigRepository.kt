package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.configuration.ScheduleConfig
import kotlinx.coroutines.flow.Flow

interface ScheduleConfigRepository {
    /** Emits [ScheduleConfig.Default] when the document is missing or can't be read. */
    fun getScheduleConfig(): Flow<ScheduleConfig>
}
