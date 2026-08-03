package com.casavirupa.voluntariart.shared.data.repositories

import com.casavirupa.voluntariart.shared.data.FirestoreUser
import com.casavirupa.voluntariat.shared.domain.UserRepository
import com.casavirupa.voluntariat.shared.model.user.User
import com.casavirupa.voluntariat.shared.model.user.UserId
import dev.gitlive.firebase.firestore.FirebaseFirestore

class FirebaseUserRepository(
    private val firestore: FirebaseFirestore,
) : UserRepository {
    override suspend fun getAllUsers(): Result<List<User>> = runCatching {
        firestore
            .collection("users")
            .get()
            .documents
            .map { it.data<FirestoreUser>().toDomainModel(it.id) }
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
    )