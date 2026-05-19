plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.dependencies"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.authentication)
            implementation(projects.features.calendar)
            implementation(projects.features.history)
            implementation(projects.features.profile)
            implementation(projects.shared.common)
            implementation(projects.shared.core)
            implementation(projects.shared.data)
        }
    }
}