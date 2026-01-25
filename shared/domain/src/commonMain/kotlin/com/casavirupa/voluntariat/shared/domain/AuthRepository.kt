package com.casavirupa.voluntariat.shared.domain

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun updateNewPassword(password: String): Result<Unit>

    suspend fun isLoggedIn(): Boolean
}