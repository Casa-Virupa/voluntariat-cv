plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.domain"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.model)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
    }
}