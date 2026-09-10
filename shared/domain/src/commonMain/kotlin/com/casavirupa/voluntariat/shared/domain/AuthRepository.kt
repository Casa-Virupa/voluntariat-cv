package com.casavirupa.voluntariat.shared.domain

import com.casavirupa.voluntariat.shared.model.user.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>

    suspend fun updateNewPassword(actualPassword: String, newPassword: String): Result<Unit>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun getCurrentUser(): Result<User>

    fun getCurrentUserFlow(): Flow<User>

    /** The volunteer's own food handler certificate flag (`users/{uid}.food_handler_certificate`). */
    suspend fun setFoodHandlerCertificate(hasCertificate: Boolean): Result<Unit>

    suspend fun logOut(): Result<Unit>
}
