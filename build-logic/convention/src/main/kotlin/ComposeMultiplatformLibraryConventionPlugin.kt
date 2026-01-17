import com.casavirupa.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformLibraryConventionPlugin : Plugin<Project> {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            val compose = extensions.getByType<ComposeExtension>().dependencies

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets {
                    androidMain.dependencies {
                        implementation(compose.preview)
                        implementation(compose.uiTooling)
                    }
                    commonMain.dependencies {
                        implementation(compose.runtime)
                        implementation(compose.foundation)
                        implementation(compose.material3)
                        implementation(compose.ui)
                        implementation(compose.components.resources)
                        implementation(compose.components.uiToolingPreview)

                        implementation(libs.findLibrary("androidx.lifecycle.viewmodelCompose").get())
                        implementation(libs.findLibrary("androidx.lifecycle.runtimeCompose").get())
                        implementation(libs.findLibrary("koin.compose").get())
                        implementation(libs.findLibrary("koin.compose.viewmodel").get())
                    }
                }
            }
        }
    }
}