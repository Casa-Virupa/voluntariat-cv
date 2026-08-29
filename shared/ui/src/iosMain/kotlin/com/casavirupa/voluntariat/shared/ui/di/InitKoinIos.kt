package com.casavirupa.voluntariat.shared.ui.di

import com.casavirupa.voluntariat.shared.core.BuildEnvironment
import com.casavirupa.voluntariat.shared.dependencies.initKoin
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

fun initIOS(buildEnvironment: BuildEnvironment) {
    Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
    initKoin(buildEnvironment)
}