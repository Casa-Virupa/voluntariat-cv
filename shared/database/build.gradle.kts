plugins {
    alias(libs.plugins.voluntariatcv.kotlin.multiplatform.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidLibrary {
        namespace = "com.casavirupa.voluntariat.shared.database"
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.sqldelight.android)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
        }
    }
}

sqldelight {
    databases {
        register("CVDatabase") {
            packageName.set("com.casavirupa.voluntariat.database")
        }
    }
}
