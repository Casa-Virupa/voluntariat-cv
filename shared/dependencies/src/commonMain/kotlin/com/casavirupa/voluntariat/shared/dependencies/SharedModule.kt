package com.casavirupa.voluntariat.shared.dependencies

import com.casavirupa.voluntariart.shared.data.di.dataModule
import com.casavirupa.voluntariat.features.authentication.di.authModule
import com.casavirupa.voluntariat.features.calendar.di.calendarModule
import com.casavirupa.voluntariat.features.history.di.historyModule
import com.casavirupa.voluntariat.shared.common.di.commonModule
import com.casavirupa.voluntariat.shared.core.di.coreModule
import org.koin.dsl.module

val sharedModule = module {
    includes(
        authModule,
        calendarModule,
        historyModule,
        commonModule,
        coreModule,
        dataModule,
    )
}