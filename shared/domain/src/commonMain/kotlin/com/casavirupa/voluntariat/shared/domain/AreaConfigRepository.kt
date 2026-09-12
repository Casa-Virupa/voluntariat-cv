package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.configuration.AreaConfig
import kotlinx.coroutines.flow.Flow

interface AreaConfigRepository {
    /** Emits [AreaConfig.Empty] when the document is missing or can't be read. */
    fun getAreaConfig(): Flow<AreaConfig>
}
