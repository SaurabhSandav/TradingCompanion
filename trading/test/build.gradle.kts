plugins {
    alias(libs.plugins.kotlin.multiplatform)
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

        optIn.addAll(
            "kotlin.time.ExperimentalTime",
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.trading.core)
            api(projects.trading.broker)

            implementation(kotlin("test"))

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.softwork.kotlinxSerializationCsv)
        }
    }
}
