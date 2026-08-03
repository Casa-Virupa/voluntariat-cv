package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.configuration.InterestLinks
import kotlinx.coroutines.flow.Flow

interface InterestLinksRepository {
    fun getInterestLinks(): Flow<InterestLinks>
}
