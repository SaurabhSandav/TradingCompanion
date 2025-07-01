plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.sqldelight)
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

        freeCompilerArgs.addAll(
            "-Xconsistent-data-class-copy-visibility",
        )
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.paging)
            api(projects.trading.core)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)

            // KotlinX DateTime
            implementation(libs.kotlinx.datetime)

            // Apache Tika
            implementation(libs.apache.tika.core)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.sqliteDriver)
            implementation(libs.sqldelight.primitiveAdapters)
            implementation(libs.sqldelight.coroutinesExtensions)

            // Jetpack Paging
            implementation(libs.jetpack.paging.common)
        }

        commonTest.dependencies {

            implementation(projects.trading.test)

            implementation(kotlin("test"))

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

sqldelight {
    databases {

        create("TradesDB") {
            packageName = "com.saurabhsandav.trading.record"
            srcDirs("src/commonMain/sqldelight/trades")
            schemaOutputDirectory = file("src/commonMain/sqldelight/trades")
            dialect(libs.sqldelight.dialect.sqlite338)
        }
    }
}
