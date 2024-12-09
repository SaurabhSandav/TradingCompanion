plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.jetbrains.compose)
}

dependencies {

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
}
