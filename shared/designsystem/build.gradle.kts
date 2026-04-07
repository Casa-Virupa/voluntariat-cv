plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
    alias(libs.plugins.voluntariatcv.compose.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.designsystem"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(libs.kotlinx.datetime)
        }
    }
}