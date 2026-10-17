package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.FirestoreUser
import com.casavirupa.voluntariat.shared.domain.UserRepository
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore,
) : UserRepository {
    // The day detail asks for the whole collection on every open just to resolve names, which
    // change rarely: serve it from memory for a while (this repository is a Koin single).
    private val cacheMutex = Mutex()
    private var cache: Pair<Instant, List<User>>? = null

    override suspend fun getAllUsers(): Result<List<User>> = cacheMutex.withLock {
        val now = Clock.System.now()
        cache
            ?.takeIf { (cachedAt, _) -> now - cachedAt < CACHE_TTL }
            ?.let { (_, users) -> return@withLock Result.success(users) }
        runCatching {
            firestore
                .collection("users")
                .get()
                .documents
                .map { it.data<FirestoreUser>().toDomainModel(it.id) }
        }.onSuccess { users -> cache = now to users }
    }

    companion object {
        val CACHE_TTL = 10.minutes
    }
}

private fun FirestoreUser.toDomainModel(docId: String) =
    User(
        id = UserId(docId),
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
