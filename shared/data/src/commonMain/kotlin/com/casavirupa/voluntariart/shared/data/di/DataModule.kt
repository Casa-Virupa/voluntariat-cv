package com.casavirupa.voluntariart.shared.data.di

import com.casavirupa.voluntariart.shared.data.FirebaseAuthRepository
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single<FirebaseAuth> { Firebase.auth }

    singleOf(::FirebaseAuthRepository) bind AuthRepository::class
}