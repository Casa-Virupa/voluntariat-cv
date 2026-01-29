package com.casavirupa.voluntariat.shared.dependencies

import com.casavirupa.voluntariart.shared.data.di.dataModule
import com.casavirupa.voluntariat.features.authentication.di.authModule
import com.casavirupa.voluntariat.shared.common.di.commonModule
import org.koin.dsl.module

val sharedModule = module {
    includes(authModule, commonModule, dataModule)
}