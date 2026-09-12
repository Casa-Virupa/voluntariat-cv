package com.casavirupa.voluntariat.features.calendar.di

import com.casavirupa.voluntariat.features.calendar.viewmodels.CalendarViewModel
import com.casavirupa.voluntariat.features.calendar.viewmodels.DayDetailViewModel
import com.casavirupa.voluntariat.features.calendar.viewmodels.ReservationFormViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val calendarModule = module {
    viewModelOf(::CalendarViewModel)
    viewModelOf(::ReservationFormViewModel)
    viewModelOf(::DayDetailViewModel)
}
