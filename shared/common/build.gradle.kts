plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
    alias(libs.plugins.voluntariatcv.compose.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.common"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.domain)
        }
    }
}