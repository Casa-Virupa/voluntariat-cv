package com.casavirupa.voluntariat.shared.dependencies

import com.casavirupa.voluntariart.shared.data.di.dataModule
import com.casavirupa.voluntariat.features.authentication.di.authModule
import org.koin.dsl.module

val sharedModule = module {
    includes(authModule, dataModule)
}