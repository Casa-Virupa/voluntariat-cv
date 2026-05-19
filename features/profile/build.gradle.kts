plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
    alias(libs.plugins.voluntariatcv.compose.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.features.profile"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.common)
            implementation(projects.shared.core)
            implementation(projects.shared.designsystem)
            implementation(projects.shared.domain)

            implementation(libs.kotlinx.datetime)
        }
    }
}