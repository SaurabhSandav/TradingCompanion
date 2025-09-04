package com.saurabhsandav.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

class ComposeConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {

        with(pluginManager) {
            apply(libs.findPlugin("kotlin-plugin-compose").get().get().pluginId)
        }

        extensions.configure<ComposeCompilerGradlePluginExtension> {

            stabilityConfigurationFiles.addAll(
                parent!!.layout.projectDirectory.file("compose-stability.conf"),
            )

            // Trigger this with:
            // ./gradlew build -PenableComposeCompilerReports --rerun-tasks
            if (project.providers.gradleProperty("enableComposeCompilerReports").isPresent) {
                val composeReports = layout.buildDirectory.map { it.dir("reports").dir("compose") }
                reportsDestination.set(composeReports)
                metricsDestination.set(composeReports)
            }
        }
    }
}
