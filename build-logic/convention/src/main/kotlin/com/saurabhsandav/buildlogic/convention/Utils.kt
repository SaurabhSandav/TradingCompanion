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
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

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

fun Project.disableWasmJsToolingDownload() {

    // Disable Yarn, Node.js download
    // Downloading them conflicts with RepositoriesMode.FAIL_ON_PROJECT_REPOS

    project.plugins.withType<WasmYarnPlugin> {
        project.the<WasmYarnRootEnvSpec>().download.set(false)
    }

    project.plugins.withType<WasmNodeJsPlugin> {
        project.the<WasmNodeJsEnvSpec>().download.set(false)
    }
}
