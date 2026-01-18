package com.casavirupa.voluntariat.android

import android.app.Application
import com.casavirupa.voluntariat.shared.core.BuildEnvironment
import com.casavirupa.voluntariat.shared.dependencies.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class VoluntariatApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val environment = when (BuildConfig.BUILD_TYPE) {
            "dev" -> BuildEnvironment.Dev
            else -> BuildEnvironment.Prod
        }

        initKoin(environment) {
            androidLogger()
            androidContext(this@VoluntariatApplication)
        }
    }
}