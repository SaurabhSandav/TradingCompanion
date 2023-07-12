import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.gradle.versions.checker)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.jetbrains.compose)
    id("app.cash.sqldelight") version "2.0.0"
    alias(libs.plugins.buildKonfig)
}

group = "com.saurabhsandav.apps"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

allprojects {
    tasks.withType(org.jetbrains.kotlin.gradle.dsl.KotlinCompile::class.java).configureEach {
        kotlinOptions {
            // Trigger this with:
            // ./gradlew assembleRelease -PenableMultiModuleComposeReports=true --rerun-tasks
            if (project.findProperty("enableMultiModuleComposeReports") == "true") {
                freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + rootProject.buildDir.absolutePath + "/compose_metrics/")
                freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + rootProject.buildDir.absolutePath + "/compose_metrics/")
            }
        }
    }
}

kotlin {

    jvm {

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }

        compilations.all {
            kotlinOptions {
                jvmTarget = "18"
            }
        }
    }

    sourceSets {

        all {

            languageSettings {

                progressiveMode = true

                listOf(
                    "kotlin.contracts.ExperimentalContracts",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "androidx.compose.foundation.ExperimentalFoundationApi",
                    "androidx.compose.ui.ExperimentalComposeUiApi",
                    "androidx.compose.animation.ExperimentalAnimationApi",
                    "androidx.compose.material.ExperimentalMaterialApi",
                    "androidx.compose.material3.ExperimentalMaterial3Api",
                    "com.russhwolf.settings.ExperimentalSettingsApi",
                ).forEach { optIn(it) }
            }
        }

        val jvmMain by getting {
            dependencies {
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
                implementation("app.cash.sqldelight:runtime:2.0.0")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.0")
                implementation("app.cash.sqldelight:primitive-adapters:2.0.0")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.0")

                implementation(libs.molecule.runtime)

                // Multiplatform Settings
                implementation(libs.multiplatformSettings.core)
                implementation(libs.multiplatformSettings.coroutines)

                // kotlin-csv
                implementation(libs.kotlinCsvJvm)

                // JavaFx
                implementation("org.openjfx:javafx-base:20.0.1:linux")
                implementation("org.openjfx:javafx-controls:20.0.1:linux")
                implementation("org.openjfx:javafx-graphics:20.0.1:linux")
                implementation("org.openjfx:javafx-media:20.0.1:linux")
                implementation("org.openjfx:javafx-swing:20.0.1:linux")
                implementation("org.openjfx:javafx-web:20.0.1:linux")

                // Krypto
                implementation("com.soywiz.korlibs.krypto:krypto:4.0.9")

                // kotlin-result
                implementation(libs.kotlinResult)
                implementation(libs.kotlinResult.coroutines)

                // AppDirs
                implementation(libs.appdirs)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose {
    kotlinCompilerPlugin.set(libs.jetpack.compose.compiler.map { it.toString() })
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
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.0")
        }

        create("TradesDB") {
            packageName = "com.saurabhsandav.core.trades"
            srcDirs("src/commonMain/sqldelight/trades")
            schemaOutputDirectory = file("build/dbs")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.0")
        }

        create("CandleDB") {
            packageName = "com.saurabhsandav.core.trading.data"
            srcDirs("src/commonMain/sqldelight/candles")
            schemaOutputDirectory = file("build/dbs")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.0")
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
