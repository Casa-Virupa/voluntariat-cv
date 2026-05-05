package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.user.User

interface UserRepository {
    suspend fun getAllUsers(): Result<List<User>>
}