package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.FirestoreUser
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.model.user.SpecificArea
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import com.casavirupa.voluntariat.shared.model.user.UserRole
import com.casavirupa.voluntariat.shared.model.user.UserVolunteerType
import co.touchlab.kermit.Logger
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.LocalDate

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

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        runCatching {
            auth.sendPasswordResetEmail(email.trim())
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
            }.catch { error ->
                Logger.e(error, LOG_TAG) { "Error in getCurrentUserFlow" }
            }
    }

    override suspend fun setFoodHandlerCertificate(hasCertificate: Boolean): Result<Unit> =
        runCatching {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(NullPointerException("User authentication failed"))
            firestore
                .collection("users")
                .document(uid)
                .updateFields {
                    "food_handler_certificate" to hasCertificate
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
        role = role.toUserRole(volunteerType),
        volunteerType = volunteerType.toVolunteerType(),
        hasOnboardingCompleted = hasOnboardingCompleted,
        specificAreas = specificAreas.map { it.toSpecificArea() },
        isMember = isMember,
        stayStart = stayStart.toLocalDateOrNull(),
        stayEnd = stayEnd.toLocalDateOrNull(),
        hasFoodHandlerCertificate = hasFoodHandlerCertificate,
    )

fun String?.toLocalDateOrNull(): LocalDate? =
    this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun String.toUserRole(volunteerType: String?) =
    when (this) {
        "volunteer" -> {
            val type = volunteerType.toVolunteerType()
            if (type != null) {
                UserRole.Volunteer(type)
            } else {
                UserRole.Unknown
            }
        }
        "area_responsible" -> UserRole.AreaResponsible
        "coordination_team" -> UserRole.CoordinationTeam
        else -> UserRole.Unknown
    }

fun String?.toVolunteerType() =
    when (this) {
        "habitual" -> UserVolunteerType.Habitual
        "mitra" -> UserVolunteerType.Mitra
        "long_stay" -> UserVolunteerType.LongStay
        else -> null
    }

fun String.toSpecificArea() =
    when (this) {
        "animals" -> SpecificArea.Animals
        "shop" -> SpecificArea.Shop
        "technical_and_audiovisual" -> SpecificArea.Audiovisual
        "communication" -> SpecificArea.Communication
        "volunteer_coordination" -> SpecificArea.VolunteerCoordination
        "kitchen" -> SpecificArea.Kitchen
        "graphical_design" -> SpecificArea.GraphicalDesign
        "virupa_editions" -> SpecificArea.VirupaEditions
        "gardening" -> SpecificArea.ExteriorsAndGardening
        "grove" -> SpecificArea.Grove
        "registrations" -> SpecificArea.Registrations
        "labor" -> SpecificArea.Labor
        "maintenance" -> SpecificArea.Maintenance
        "works" -> SpecificArea.Works
        "pedagogical" -> SpecificArea.Pedagogical
        "community_health" -> SpecificArea.Health
        "grants" -> SpecificArea.Grants
        "technical_and_programming" -> SpecificArea.TechnicalAndProgramming
        "temple" -> SpecificArea.Temple
        "technical_and_texts" -> SpecificArea.Texts
        "transcriptions" -> SpecificArea.Transcriptions
        else -> SpecificArea.Unknown
    }

private const val LOG_TAG = "FirebaseAuthRepository"