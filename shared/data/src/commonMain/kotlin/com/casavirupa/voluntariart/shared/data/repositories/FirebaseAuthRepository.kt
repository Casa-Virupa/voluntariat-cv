package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.FirestoreUser
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.firestore.FirebaseFirestore

class FirebaseAuthRepository(
    val auth: FirebaseAuth,
    val firestore: FirebaseFirestore,
) : AuthRepository {
    override suspend fun signIn(email: String, password: String): Result<User> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email, password)
            val authUser = result.user
                ?: return Result.failure(NullPointerException("User authentication failed"))
            return findUserFromFirestore(authUser)
        }

    override suspend fun updateNewPassword(password: String): Result<Unit> =
        runCatching {
            auth.currentUser?.updatePassword(password)
        }.mapCatching {
            firestore
                .collection("users")
                .document(auth.currentUser?.uid!!)
                .updateFields {
                    "onboarding_completed" to true
                }
        }

    override suspend fun getCurrentUser(): Result<User> {
        val authUser = auth.currentUser
            ?: return Result.failure(NullPointerException("User authentication failed"))
        return findUserFromFirestore(authUser)
    }

    private suspend fun findUserFromFirestore(user: FirebaseUser): Result<User> {
        val email = user.email
            ?: return Result.failure(NullPointerException("User email not found"))
        return firestore
            .collection("users")
            .document(user.uid)
            .get()
            .data<FirestoreUser>()
            .let { Result.success(it.toDomainModel(email)) }
    }
}

private fun FirestoreUser.toDomainModel(email: String): User =
    User(
        id = UserId(id),
        name = name,
        email = email,
        role = role.toUserRole(),
        hasOnboardingCompleted = hasOnboardingCompleted,
    )

private fun String.toUserRole() =
    when (this) {
        "volunteer" -> UserRole.Volunteer
        "area_responsible" -> UserRole.AreaResponsible
        "coordination_team" -> UserRole.CoordinationTeam
        else -> UserRole.Unknown
    }