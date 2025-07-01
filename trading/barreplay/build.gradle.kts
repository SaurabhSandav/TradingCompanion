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

        optIn = listOf(
            "kotlin.time.ExperimentalTime",
        )
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.trading.core)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {

            implementation(projects.trading.test)

            implementation(kotlin("test"))
        }
    }
}
