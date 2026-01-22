plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.data"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
        }
        commonMain.dependencies {
            implementation(projects.shared.domain)
            implementation(libs.gitlive.firebase.auth)
        }
    }
}