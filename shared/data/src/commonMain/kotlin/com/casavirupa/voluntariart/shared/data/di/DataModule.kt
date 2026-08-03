package com.casavirupa.voluntariart.shared.data.di

import com.casavirupa.voluntariart.shared.data.repositories.FirebaseAuthRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebaseCommitmentRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebaseInterestLinksRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebaseLedgerRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebasePriceRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebaseUserRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebaseVolunteerRepository
import com.casavirupa.voluntariart.shared.data.repositories.GoogleCalendarRepository
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.CommitmentRepository
import com.casavirupa.voluntariat.shared.domain.InterestLinksRepository
import com.casavirupa.voluntariat.shared.domain.LedgerRepository
import com.casavirupa.voluntariat.shared.domain.PriceRepository
import com.casavirupa.voluntariat.shared.domain.UserRepository
import com.casavirupa.voluntariat.shared.domain.VolunteerRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single<FirebaseAuth> { Firebase.auth }
    single<FirebaseFirestore> { Firebase.firestore }

    singleOf(::FirebaseAuthRepository) bind AuthRepository::class
    singleOf(::GoogleCalendarRepository) bind CalendarRepository::class
    singleOf(::FirebaseUserRepository) bind UserRepository::class
    singleOf(::FirebaseVolunteerRepository) bind VolunteerRepository::class
    singleOf(::FirebasePriceRepository) bind PriceRepository::class
    singleOf(::FirebaseCommitmentRepository) bind CommitmentRepository::class
    singleOf(::FirebaseLedgerRepository) bind LedgerRepository::class
    singleOf(::FirebaseInterestLinksRepository) bind InterestLinksRepository::class
}