import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// FIXME IDE warns "`libs` can't be called in this context by implicit receiver"
// https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress(
    "DSL_SCOPE_VIOLATION",
    "MISSING_DEPENDENCY_CLASS",
    "UNRESOLVED_REFERENCE_WRONG_RECEIVER",
    "FUNCTION_CALL_EXPECTED"
)
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.molecule)
    alias(libs.plugins.sqldelight)
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
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.sqliteDriver)
                implementation(libs.sqldelight.coroutinesExtensions)

                // Multiplatform Settings
                implementation(libs.multiplatformSettings.core)
                implementation(libs.multiplatformSettings.coroutines)

                // kotlin-csv
                implementation(libs.kotlinCsvJvm)

                // JavaFx
                implementation("org.openjfx:javafx-base:19.0.2.1:linux")
                implementation("org.openjfx:javafx-controls:19.0.2.1:linux")
                implementation("org.openjfx:javafx-graphics:19.0.2.1:linux")
                implementation("org.openjfx:javafx-media:19.0.2.1:linux")
                implementation("org.openjfx:javafx-swing:19.0.2.1:linux")
                implementation("org.openjfx:javafx-web:19.0.2.1:linux")

                // Krypto
                implementation("com.soywiz.korlibs.krypto:krypto:3.4.0")

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

    database("AppDB") {
        packageName = "com.saurabhsandav.core"
        sourceFolders = listOf("sqldelight/app")
        schemaOutputDirectory = file("build/dbs")
        dialect = "sqlite:3.25"
    }

    database("TradesDB") {
        packageName = "com.saurabhsandav.core"
        sourceFolders = listOf("sqldelight/trades")
        schemaOutputDirectory = file("build/dbs")
        dialect = "sqlite:3.25"
    }

    database("CandleDB") {
        packageName = "com.saurabhsandav.core"
        sourceFolders = listOf("sqldelight/candles")
        schemaOutputDirectory = file("build/dbs")
        dialect = "sqlite:3.25"
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
