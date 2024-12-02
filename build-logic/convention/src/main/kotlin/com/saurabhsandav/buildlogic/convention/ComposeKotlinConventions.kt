package com.saurabhsandav.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class ComposeMultiplatformAppConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {

        with(pluginManager) {
            apply(KotlinMultiplatformConventionPlugin::class)
            apply(ComposeConventionPlugin::class)
            apply(libs.findPlugin("jetbrains-compose").get().get().pluginId)
            if (!isReleaseBuild) {
                apply(libs.findPlugin("jetbrains-composeHotReload").get().get().pluginId)
            }
        }

        val enableDebugFlag = project.enableDebugFlag()

        extensions.configure<KotlinMultiplatformExtension> {

            jvm {

                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                mainRun {

                    if (enableDebugFlag) args("-D")
                }
            }
        }

        extensions.configure<ComposeExtension> {
            extensions.configure<DesktopExtension> {

                application {

                    if (enableDebugFlag) args("-D")
                }
            }
        }

        tasks.withType<ComposeHotRun>().configureEach {
            if (enableDebugFlag) args("-D")
        }

        // Fix jcef_helper file is not executable in JetBrains JCEF
        fixAppDistributablePermissions()
    }
}

class ComposeMultiplatformLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {

        with(pluginManager) {
            apply(KotlinMultiplatformConventionPlugin::class)
            apply(ComposeConventionPlugin::class)
            apply(libs.findPlugin("jetbrains-compose").get().get().pluginId)
        }
    }
}

val Project.isReleaseBuild: Boolean
    get() {

        val releaseTasksPrefixes = listOf(
            "createRelease",
            "runRelease",
            "packageRelease",
        )

        return gradle.startParameter.taskNames.any { taskName ->
            releaseTasksPrefixes.any { prefix -> taskName.contains(prefix) }
        }
    }

private fun Project.enableDebugFlag(): Boolean {

    // Enable debug mode by default
    val debugModeProperty = findProperty("debugMode") ?: return true

    return debugModeProperty.toString().equals("true", ignoreCase = true)
}
