package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.user.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>

    suspend fun updateNewPassword(actualPassword: String, newPassword: String): Result<Unit>

    suspend fun getCurrentUser(): Result<User>

    fun getCurrentUserFlow(): Flow<User>

    suspend fun logOut(): Result<Unit>
    suspend fun resetPassword(value: String)
}