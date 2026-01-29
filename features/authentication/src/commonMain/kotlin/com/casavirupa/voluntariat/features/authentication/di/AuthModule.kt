package com.casavirupa.voluntariat.features.authentication.di

import com.casavirupa.voluntariat.features.authentication.password.CreatePasswordViewModel
import com.casavirupa.voluntariat.features.authentication.signin.SignInViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    viewModelOf(::SignInViewModel)
    viewModelOf(::CreatePasswordViewModel)
}