package com.casavirupa.voluntariat.shared.common.di

import com.casavirupa.voluntariat.shared.common.AppViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonModule = module {
    viewModelOf(::AppViewModel)
}