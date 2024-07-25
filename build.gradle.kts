plugins {
    alias(libs.plugins.gradle.versions.checker)
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.kotlin.plugin.compose) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.buildKonfig) apply false
    id("de.undercouch.download") version "5.6.0" apply false
}
