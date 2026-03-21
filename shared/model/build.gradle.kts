plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.model"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
    }
}