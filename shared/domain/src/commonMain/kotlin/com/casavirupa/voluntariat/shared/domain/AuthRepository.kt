package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.user.User

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>

    suspend fun updateNewPassword(actualPassword: String, newPassword: String): Result<Unit>

    suspend fun getCurrentUser(): Result<User>
}