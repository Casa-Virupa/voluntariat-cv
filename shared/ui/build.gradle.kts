plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
    alias(libs.plugins.voluntariatcv.compose.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.ui"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.features.authentication)
            implementation(projects.features.calendar)
            implementation(projects.features.history)
            implementation(projects.features.profile)
            implementation(projects.shared.common)
            implementation(projects.shared.core)
            implementation(projects.shared.dependencies)
            implementation(projects.shared.designsystem)
        }
        iosMain.dependencies {
            implementation(projects.shared.dependencies)
            implementation(projects.shared.common)
        }
    }
}