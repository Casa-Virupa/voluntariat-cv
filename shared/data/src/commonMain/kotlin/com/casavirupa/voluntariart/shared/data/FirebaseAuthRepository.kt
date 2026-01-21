package com.casavirupa.voluntariart.shared.data

import com.casavirupa.voluntariat.shared.domain.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth

class FirebaseAuthRepository(val firebaseAuth: FirebaseAuth) : AuthRepository {
    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password)
            return if (result.user != null) {
                Result.success(Unit)
            } else {
                Result.failure(NullPointerException("User is null"))
            }
        }
}