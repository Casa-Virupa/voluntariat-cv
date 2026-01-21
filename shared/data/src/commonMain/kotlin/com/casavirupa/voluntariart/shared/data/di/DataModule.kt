package com.casavirupa.voluntariart.shared.data.di

import com.casavirupa.voluntariart.shared.data.FirebaseAuthRepository
import com.casavirupa.voluntariat.shared.domain.AuthRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    singleOf(::FirebaseAuthRepository) bind AuthRepository::class
}