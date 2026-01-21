package com.casavirupa.voluntariat.shared.domain

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
}