plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.core"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.navigation3)
            implementation(libs.compose.viewModel.navigation3)
            implementation(libs.kotlinx.datetime)
        }
    }
}