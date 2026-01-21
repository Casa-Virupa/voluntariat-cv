package com.casavirupa.voluntariat.shared.dependencies

import com.casavirupa.voluntariart.shared.data.di.dataModule
import org.koin.dsl.module

val sharedModule = module {
    includes(dataModule)
}