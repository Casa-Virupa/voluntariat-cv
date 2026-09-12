plugins {
    alias(libs.plugins.voluntariatcv.android.application)
    alias(libs.plugins.voluntariatcv.android.application.compose)
    alias(libs.plugins.voluntariatcv.android.application.flavors)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.casavirupa.voluntariat.android"

    defaultConfig {
        applicationId = "com.casavirupa.voluntariat.android"
        versionCode = 1
        versionName = "1.1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(projects.shared.common)
    implementation(projects.shared.core)
    implementation(projects.shared.dependencies)
    implementation(projects.shared.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.koin.android)
}