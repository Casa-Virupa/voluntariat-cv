package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.user.User

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>

    suspend fun updateNewPassword(password: String): Result<Unit>

    suspend fun getCurrentUser(): Result<User>
}