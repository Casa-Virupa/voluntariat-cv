plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
    alias(libs.plugins.voluntariatcv.compose.multiplatform.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.common)
            implementation(projects.shared.core)
            implementation(projects.shared.designsystem)
            implementation(projects.shared.domain)

            implementation(libs.calf.ui)
            implementation(libs.kotlinx.datetime)
        }
    }
}