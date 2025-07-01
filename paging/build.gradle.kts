plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.jetbrains.compose)
}

group = "com.saurabhsandav.trading"
version = "1.0-SNAPSHOT"

kotlin {

    jvm {

        compilerOptions.freeCompilerArgs.add("-Xjdk-release=21")

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    compilerOptions {

        progressiveMode = true
    }

    sourceSets {

        commonMain.dependencies {

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Jetpack Paging
            implementation(libs.jetpack.paging.common)

            // SQLDelight
            implementation(libs.sqldelight.runtime)

            // Compose
            implementation(compose.runtime)
        }
    }
}
