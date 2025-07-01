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
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.trading.core)
        }
    }
}
