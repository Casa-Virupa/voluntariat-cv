package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.FirestoreUser
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull

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

    override suspend fun updateNewPassword(
        actualPassword: String,
        newPassword: String,
    ): Result<Unit> = runCatching {
        val authUser = auth.currentUser
            ?: return Result.failure(NullPointerException("User authentication failed"))
        authUser.reauthenticate(
            EmailAuthProvider.credential(authUser.email.orEmpty(), actualPassword)
        )
        authUser.updatePassword(newPassword)
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

    override fun getCurrentUserFlow(): Flow<User> {
        val userId = auth.currentUser?.uid ?: return emptyFlow()
        val email = auth.currentUser?.email ?: return emptyFlow()

        return firestore
            .collection("users")
            .document(userId)
            .snapshots
            .mapNotNull { snapshot ->
                if (snapshot.exists) {
                    snapshot
                        .data<FirestoreUser>()
                        .toDomainModel(UserId(userId), email)
                } else {
                    null
                }
            }
    }

    override suspend fun logOut(): Result<Unit> =
        runCatching {
            auth.signOut()
        }

    private suspend fun findUserFromFirestore(user: FirebaseUser): Result<User> {
        val email = user.email
            ?: return Result.failure(NullPointerException("User email not found"))
        return runCatching {
            firestore
                .collection("users")
                .document(user.uid)
                .get()
                .data<FirestoreUser>()
                .toDomainModel(UserId(user.uid), email)
        }
    }
}

private fun FirestoreUser.toDomainModel(id: UserId, email: String): User =
    User(
        id = id,
        name = name,
        email = email,
        role = role.toUserRole(),
        hasOnboardingCompleted = hasOnboardingCompleted,
        specificAreas = specificAreas.map { it.toSpecificArea() }
    )

fun String.toUserRole() =
    when (this) {
        "volunteer" -> UserRole.Volunteer
        "area_responsible" -> UserRole.AreaResponsible
        "coordination_team" -> UserRole.CoordinationTeam
        else -> UserRole.Unknown
    }

fun String.toSpecificArea() =
    when (this) {
        "animals" -> SpecificArea.Animals
        "shop" -> SpecificArea.Shop
        "communication" -> SpecificArea.Communication
        "volunteer_coordination" -> SpecificArea.VolunteerCoordination
        "kitchen" -> SpecificArea.Kitchen
        "graphical_design" -> SpecificArea.GraphicalDesign
        "virupa_editions" -> SpecificArea.VirupaEditions
        "exterior" -> SpecificArea.Exterior
        "can_bordoi_events" -> SpecificArea.CanBordoiEvents
        "grove" -> SpecificArea.Grove
        "registrations" -> SpecificArea.Registrations
        "gardening" -> SpecificArea.Gardening
        "labor" -> SpecificArea.Labor
        "maintenance" -> SpecificArea.Maintenance
        "pedagogical" -> SpecificArea.Pedagogical
        "community_health" -> SpecificArea.CommunityHealth
        "grants" -> SpecificArea.Grants
        "temple" -> SpecificArea.Temple
        "transcriptions" -> SpecificArea.Transcriptions
        "technical_and_audiovisual" -> SpecificArea.TechnicalAndAudiovisual
        "technical_and_texts" -> SpecificArea.TechnicalAndTexts
        else -> SpecificArea.Unknown
    }