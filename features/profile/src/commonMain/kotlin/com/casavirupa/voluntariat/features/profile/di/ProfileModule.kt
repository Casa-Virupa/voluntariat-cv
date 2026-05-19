package com.casavirupa.voluntariat.features.profile.di

import com.casavirupa.voluntariat.features.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileViewModel)
}