plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.data"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.domain)
            implementation(libs.gitlive.firebase.auth)
        }
    }
}