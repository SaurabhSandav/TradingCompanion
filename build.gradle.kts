@file:Suppress("OPT_IN_USAGE")

import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.gradle.versions.checker)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildKonfig)
}

group = "com.saurabhsandav.apps"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://androidx.dev/storage/compose-compiler/repository/")
}

kotlin {

    compilerOptions {

        progressiveMode = true

        optIn.addAll(
            "kotlin.contracts.ExperimentalContracts",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.material.ExperimentalMaterialApi",
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "com.russhwolf.settings.ExperimentalSettingsApi",
        )

        // Trigger this with:
        // ./gradlew build -PenableMultiModuleComposeReports=true --rerun-tasks
        if (project.findProperty("enableMultiModuleComposeReports") == "true") {

            val path = layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath

            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$path",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$path",
            )
        }
    }

    jvm {

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {

        jvmMain.dependencies {

            implementation(compose.desktop.currentOs)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // KotlinX DateTime
            implementation(libs.kotlinx.datetime)

            // KotlinX Immutable Collections Library
            implementation(libs.kotlinx.collections.immutable)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.sqliteDriver)
            implementation(libs.sqldelight.primitiveAdapters)
            implementation(libs.sqldelight.coroutinesExtensions)

            implementation(libs.molecule.runtime)

            // Multiplatform Settings
            implementation(libs.multiplatformSettings.core)
            implementation(libs.multiplatformSettings.coroutines)

            // kotlin-csv
            implementation(libs.kotlinCsvJvm)

            // JavaFx
            implementation("org.openjfx:javafx-base:20.0.2:linux")
            implementation("org.openjfx:javafx-controls:20.0.2:linux")
            implementation("org.openjfx:javafx-graphics:20.0.2:linux")
            implementation("org.openjfx:javafx-media:20.0.2:linux")
            implementation("org.openjfx:javafx-swing:20.0.2:linux")
            implementation("org.openjfx:javafx-web:20.0.2:linux")

            // Krypto
            implementation("com.soywiz.korlibs.krypto:krypto:4.0.10")

            // kotlin-result
            implementation(libs.kotlinResult)
            implementation(libs.kotlinResult.coroutines)

            // AppDirs
            implementation(libs.appdirs)
        }

        jvmTest.dependencies {

            implementation(kotlin("test"))
        }
    }
}

@Suppress("PropertyName")
val FYERS_APP_ID: String? by project

@Suppress("PropertyName")
val FYERS_SECRET: String? by project

buildkonfig {
    packageName = ""

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "FYERS_APP_ID", FYERS_APP_ID)
        buildConfigField(FieldSpec.Type.STRING, "FYERS_SECRET", FYERS_SECRET)
    }
}

sqldelight {
    databases {

        create("AppDB") {
            packageName = "com.saurabhsandav.core"
            srcDirs("src/commonMain/sqldelight/app")
            schemaOutputDirectory = file("build/dbs")
            dialect(libs.sqldelight.dialect.sqlite338)
        }

        create("TradesDB") {
            packageName = "com.saurabhsandav.core.trades"
            srcDirs("src/commonMain/sqldelight/trades")
            schemaOutputDirectory = file("build/dbs")
            dialect(libs.sqldelight.dialect.sqlite338)
        }

        create("CandleDB") {
            packageName = "com.saurabhsandav.core.trading.data"
            srcDirs("src/commonMain/sqldelight/candles")
            schemaOutputDirectory = file("build/dbs")
            dialect(libs.sqldelight.dialect.sqlite338)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.saurabhsandav.core.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TradingCompanion"
            packageVersion = "1.0.0"
        }
    }
}
