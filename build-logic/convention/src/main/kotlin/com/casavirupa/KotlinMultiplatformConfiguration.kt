package com.casavirupa

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
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
    targets
        .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        .configureEach {
            namespace = "com.bibbib.${project.name.replace("-", ".")}"
            compileSdk = 37
            minSdk = 24

            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
            androidResources {
                enable = true
            }
            withHostTest {
                isIncludeAndroidResources = true
            }
        }
}

private fun KotlinMultiplatformExtension.iosLibraryConfiguration(bundleId: String) {
    listOf(
        // iosX64(),
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