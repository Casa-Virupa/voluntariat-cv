package com.casavirupa

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.VariantDimension

object FlavorConfigField {
    object Type {
        const val STRING = "String"
    }

    object Name {
        const val BUILD_TYPE = "BUILD_TYPE"
    }
}

@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

@Suppress("EnumEntryName")
enum class VoluntariatFlavor(
    val flavorName: String,
    val dimension: FlavorDimension,
    val applicationIdSuffix: String? = null,
) {
    dev(
        flavorName = "dev",
        dimension = FlavorDimension.contentType,
        applicationIdSuffix = ".dev",
    ),
    prod(
        flavorName = "prod",
        dimension = FlavorDimension.contentType
    ),
}

@OptIn(ExperimentalStdlibApi::class)
fun configureFlavors(commonExtension: CommonExtension<*, *, *, *, *, *>) {
    commonExtension.apply {
        FlavorDimension.values().forEach { flavorDimension ->
            flavorDimensions += flavorDimension.name
        }

        productFlavors {
            VoluntariatFlavor.values().forEach { flavor ->
                register(flavor.flavorName) {
                    dimension = flavor.dimension.name
                    setFlavorConfigBuild(flavor)

                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        if (flavor.applicationIdSuffix != null) {
                            applicationIdSuffix = flavor.applicationIdSuffix
                        }
                    }
                }
            }
        }
    }
}

private fun VariantDimension.setFlavorConfigBuild(flavor: VoluntariatFlavor) {
    buildConfigField(
        type = FlavorConfigField.Type.STRING,
        name = FlavorConfigField.Name.BUILD_TYPE,
        value = "\"${flavor.flavorName}\"",
    )
}