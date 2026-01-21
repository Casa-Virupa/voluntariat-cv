plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.dependencies"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.data)
        }
    }
}