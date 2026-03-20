package com.casavirupa.voluntariat.features.calendar.di

import com.casavirupa.voluntariat.features.calendar.CalendarViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val calendarModule = module {
    viewModelOf(::CalendarViewModel)
}