package com.casavirupa.voluntariat.shared.ui.di

import com.casavirupa.voluntariat.shared.dependencies.sharedModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(sharedModule)
    }
}