import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec

plugins {
    alias(libs.plugins.gradle.versions.checker)
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.kotlin.plugin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.jetbrains.composeHotReload) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.buildKonfig) apply false
    id("de.undercouch.download") version "5.6.0" apply false
}

tasks.register<Delete>("cleanAll") {
    delete(
        rootProject.layout.buildDirectory,
        subprojects.map { it.layout.buildDirectory },
    )
}

// region Disable Yarn, Node.js download
// Downloading them conflicts with RepositoriesMode.FAIL_ON_PROJECT_REPOS

rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootEnvSpec>().download = false
}

rootProject.plugins.withType<NodeJsPlugin> {
    rootProject.the<NodeJsEnvSpec>().download = false
}

// endregion Disable Yarn, Node.js download
