package com.casavirupa.voluntariat.features.history.di

import com.casavirupa.voluntariat.features.history.HistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val historyModule = module {
    viewModelOf(::HistoryViewModel)
}