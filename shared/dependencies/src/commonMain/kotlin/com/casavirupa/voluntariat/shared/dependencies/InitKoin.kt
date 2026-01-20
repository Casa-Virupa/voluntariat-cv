package com.casavirupa.voluntariat.shared.dependencies

import com.casavirupa.voluntariat.shared.core.BuildEnvironment
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(
    buildEnvironment: BuildEnvironment,
    appDeclaration: KoinAppDeclaration = {},
) = startKoin {
    appDeclaration()
    sharedModule.single { buildEnvironment }

    modules(sharedModule)
}