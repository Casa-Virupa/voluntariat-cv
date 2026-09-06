package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId

interface UserRepository {
    suspend fun getAllUsers(): Result<List<User>>

    /** Blank values are stored as null so the dashboard can tell "not provided" apart. */
    suspend fun updateContactDetails(
        userId: UserId,
        phone: String?,
        address: String?,
    ): Result<Unit>
}