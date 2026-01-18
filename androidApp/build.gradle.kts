plugins {
    alias(libs.plugins.voluntariatcv.android.application)
    alias(libs.plugins.voluntariatcv.android.application.compose)
    alias(libs.plugins.voluntariatcv.android.application.flavors)
}

android {
    namespace = "com.casavirupa.voluntariat.android"

    defaultConfig {
        applicationId = "com.casavirupa.voluntariat.android"
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(projects.shared.core)
    implementation(projects.shared.dependencies)
    implementation(projects.shared.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
}