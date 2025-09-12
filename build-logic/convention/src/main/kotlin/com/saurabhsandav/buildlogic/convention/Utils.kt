package com.saurabhsandav.buildlogic.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.disableJsToolingDownload() {

    // Disable Yarn, Node.js download
    // Downloading them conflicts with RepositoriesMode.FAIL_ON_PROJECT_REPOS

    project.plugins.withType<YarnPlugin> {
        project.the<YarnRootEnvSpec>().download.set(false)
    }

    project.plugins.withType<NodeJsPlugin> {
        project.the<NodeJsEnvSpec>().download.set(false)
    }
}
