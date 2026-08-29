import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.calcar.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.voluntariatcv.android.application.asProvider().get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = libs.plugins.voluntariatcv.android.application.compose.get().pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplicationFlavors") {
            id = libs.plugins.voluntariatcv.android.application.flavors.get().pluginId
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("kotlinMultiplatformLibrary") {
            id = libs.plugins.voluntariatcv.kotlin.multiplatform.library.get().pluginId
            implementationClass = "KotlinMultiplatformLibraryConventionPlugin"
        }
        register("composeMultiplatformLibrary") {
            id = libs.plugins.voluntariatcv.compose.multiplatform.library.get().pluginId
            implementationClass = "ComposeMultiplatformLibraryConventionPlugin"
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("com.android.tools.build:gradle-api:9.1.0")
        force("com.android.tools.build:builder-model:9.1.0")
        force("com.android.tools.build:gradle:9.1.0")
    }
}