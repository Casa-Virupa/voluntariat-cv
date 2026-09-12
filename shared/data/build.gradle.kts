plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
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
            implementation(projects.shared.core)
            implementation(projects.shared.database)
            implementation(projects.shared.domain)
            implementation(libs.gitlive.firebase.auth)
            implementation(libs.gitlive.firebase.firestore)

            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(project.dependencies.platform(libs.ktor.bom))
            implementation(libs.ktor.client.core)
        }
    }
}