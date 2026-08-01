package com.casavirupa

import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKotlinMultiplatform(
    extension: KotlinMultiplatformExtension
) {
    extension.apply {
        androidLibraryConfiguration()
        iosLibraryConfiguration(
            bundleId = "com.casavirupa.voluntariat${path.replace(':', '.')}",
        )
    }
}

private fun KotlinMultiplatformExtension.androidLibraryConfiguration() {
    androidLibrary {
        compileSdk = 36
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
    }
}

private fun KotlinMultiplatformExtension.iosLibraryConfiguration(bundleId: String) {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
            binaryOption("bundleId", bundleId)
        }
    }
}