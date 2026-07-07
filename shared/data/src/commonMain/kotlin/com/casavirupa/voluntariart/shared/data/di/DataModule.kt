package com.casavirupa.voluntariart.shared.data.di

import com.casavirupa.voluntariart.shared.data.repositories.FirebaseAuthRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebasePaymentRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebaseUserRepository
import com.casavirupa.voluntariart.shared.data.repositories.FirebaseVolunteerRepository
import com.casavirupa.voluntariart.shared.data.repositories.GoogleCalendarRepository
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import com.casavirupa.voluntariat.shared.domain.CalendarRepository
import com.casavirupa.voluntariat.shared.domain.PaymentRepository
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
    singleOf(::FirebasePaymentRepository) bind PaymentRepository::class
}