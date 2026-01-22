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
            implementation(projects.shared.dependencies)
        }
        iosMain.dependencies {
            implementation(projects.shared.dependencies)
        }
    }
}