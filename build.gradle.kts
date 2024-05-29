import com.codingfeline.buildkonfig.compiler.FieldSpec
import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.gradle.versions.checker)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildKonfig)
    id("de.undercouch.download") version "5.6.0"
}

group = "com.saurabhsandav.apps"
version = "1.0-SNAPSHOT"

configurations.configureEach {

    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jetbrains.compose.material:material"))
            .using(module("org.jetbrains.compose.material3:material3:${libs.versions.jetbrainsCompose.get()}"))
            .because("Material 3 is newer")
    }
}

kotlin {

    jvmToolchain(17)

    jvm {

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {

        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xcontext-receivers",
        )
    }

    sourceSets {

        configureEach {

            languageSettings {

                progressiveMode = true

                listOf(
                    "kotlin.contracts.ExperimentalContracts",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "kotlinx.coroutines.FlowPreview",
                    "androidx.compose.foundation.ExperimentalFoundationApi",
                    "androidx.compose.foundation.layout.ExperimentalLayoutApi",
                    "androidx.compose.ui.ExperimentalComposeUiApi",
                    "androidx.compose.animation.ExperimentalAnimationApi",
                    "androidx.compose.material3.ExperimentalMaterial3Api",
                    "com.russhwolf.settings.ExperimentalSettingsApi",
                ).forEach { optIn(it) }
            }
        }

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
            implementation(libs.ktor.server.netty)
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
            implementation("org.openjfx:javafx-base:22.0.1:linux")
            implementation("org.openjfx:javafx-controls:22.0.1:linux")
            implementation("org.openjfx:javafx-graphics:22.0.1:linux")
            implementation("org.openjfx:javafx-media:22.0.1:linux")
            implementation("org.openjfx:javafx-swing:22.0.1:linux")
            implementation("org.openjfx:javafx-web:22.0.1:linux")

            // Krypto
            implementation("dev.whyoleg.cryptography:cryptography-core:0.3.1")
            implementation("dev.whyoleg.cryptography:cryptography-provider-jdk:0.3.1")

            // kotlin-result
            implementation(libs.kotlinResult)
            implementation(libs.kotlinResult.coroutines)

            // AppDirs
            implementation(libs.appdirs)

            // Kermit
            implementation(libs.kermit)

            // Compose Multiplatform File Picker
            implementation(libs.mpfilepicker)

            // JCEF MAVEN
            implementation(libs.jcefMaven)

            // compose-richtext
            implementation("com.halilibo.compose-richtext:richtext-ui-material3:1.0.0-alpha01")
            implementation("com.halilibo.compose-richtext:richtext-commonmark:1.0.0-alpha01")

            // Jetpack Paging
            implementation(libs.jetpack.paging.common)
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
            schemaOutputDirectory = file("src/commonMain/sqldelight/app")
            dialect(libs.sqldelight.dialect.sqlite338)
        }

        create("TradesDB") {
            packageName = "com.saurabhsandav.core.trades"
            srcDirs("src/commonMain/sqldelight/trades")
            schemaOutputDirectory = file("src/commonMain/sqldelight/trades")
            dialect(libs.sqldelight.dialect.sqlite338)
        }

        create("CandleDB") {
            packageName = "com.saurabhsandav.core.trading.data"
            srcDirs("src/commonMain/sqldelight/candles")
            schemaOutputDirectory = file("src/commonMain/sqldelight/candles")
            dialect(libs.sqldelight.dialect.sqlite338)
        }
    }
}

composeCompiler {
    enableStrongSkippingMode = true
    stabilityConfigurationFile = layout.projectDirectory.file("compose_compiler_config.conf")

    // Trigger this with:
    // ./gradlew build -PenableComposeCompilerReports --rerun-tasks
    if (project.providers.gradleProperty("enableComposeCompilerReports").isPresent) {
        val composeReports = layout.buildDirectory.map { it.dir("reports").dir("compose") }
        reportsDestination.set(composeReports)
        metricsDestination.set(composeReports)
    }
}

compose.desktop {
    application {
        mainClass = "com.saurabhsandav.core.MainKt"
        jvmArgs += listOf(
            "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        )
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TradingCompanion"
            packageVersion = "1.0.0"
        }
    }
}

val downloadLWC by tasks.registering(Download::class) {
    src("https://unpkg.com/lightweight-charts@4.1.4/dist/lightweight-charts.standalone.production.js")
    dest("src/jvmMain/resources/charts_page")
    overwrite(false)
}

tasks.withType<ProcessResources> {
    dependsOn(downloadLWC)
}
