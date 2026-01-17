import com.casavirupa.configureKotlinMultiplatform
import com.casavirupa.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.multiplatform")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                configureKotlinMultiplatform(this)

                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.findLibrary("koin.core").get())
                    }
                    commonTest.dependencies {
                        implementation(kotlin("test"))
                        implementation(libs.findLibrary("kotlinx.coroutines.test").get())
                    }
                }
            }
        }
    }
}