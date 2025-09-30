plugins {
    `kotlin-dsl`
}

group = "com.saurabhsandav.buildlogic"

dependencies {
    compileOnly(libs.plugins.kotlin.multiplatform.asDep())
    compileOnly(libs.plugins.kotlin.plugin.compose.asDep())
    compileOnly(libs.plugins.jetbrains.compose.asDep())
    compileOnly(libs.plugins.jetbrains.composeHotReload.asDep())
}

fun Provider<PluginDependency>.asDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

kotlin {

    compilerOptions {

        progressiveMode.set(true)

        optIn.addAll(
            "org.jetbrains.kotlin.gradle.ExperimentalWasmDsl",
        )
    }
}

gradlePlugin {

    plugins {

        register("convention.kotlin.multiplatform") {
            id = "convention.kotlin.multiplatform"
            implementationClass = "com.saurabhsandav.buildlogic.convention.KotlinMultiplatformConventionPlugin"
        }

        register("convention.kotlin.plugin.compose") {
            id = "convention.kotlin.plugin.compose"
            implementationClass = "com.saurabhsandav.buildlogic.convention.ComposeConventionPlugin"
        }

        register("convention.compose-multiplatform.app") {
            id = "convention.compose-multiplatform.app"
            implementationClass = "com.saurabhsandav.buildlogic.convention.ComposeMultiplatformAppConventionPlugin"
        }

        register("convention.compose-multiplatform.library") {
            id = "convention.compose-multiplatform.library"
            implementationClass = "com.saurabhsandav.buildlogic.convention.ComposeMultiplatformLibraryConventionPlugin"
        }

        register("convention.test.resources") {
            id = "convention.test.resources"
            implementationClass = "com.saurabhsandav.buildlogic.convention.TestResourcesConventionPlugin"
        }
    }
}
