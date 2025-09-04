package com.saurabhsandav.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class ComposeMultiplatformAppConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {

        with(pluginManager) {
            apply(KotlinMultiplatformConventionPlugin::class)
            apply(ComposeConventionPlugin::class)
            apply(libs.findPlugin("jetbrains-compose").get().get().pluginId)
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
