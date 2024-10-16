import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.sqldelight)
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
            "-Xconsistent-data-class-copy-visibility",
        )
    }

    sourceSets {

        configureEach {

            languageSettings {

                progressiveMode = true

                listOf(
                    "kotlin.contracts.ExperimentalContracts",
                    "kotlin.io.path.ExperimentalPathApi",
                    "kotlin.uuid.ExperimentalUuidApi",
                    "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "kotlinx.coroutines.FlowPreview",
                    "androidx.compose.foundation.ExperimentalFoundationApi",
                    "androidx.compose.foundation.layout.ExperimentalLayoutApi",
                    "androidx.compose.ui.ExperimentalComposeUiApi",
                    "androidx.compose.animation.ExperimentalAnimationApi",
                    "androidx.compose.material3.ExperimentalMaterial3Api",
                    "com.russhwolf.settings.ExperimentalSettingsApi",
                    "com.russhwolf.settings.ExperimentalSettingsImplementation",
                ).forEach { optIn(it) }
            }
        }

        commonMain.dependencies {

            implementation(projects.fyersApi)
            implementation(projects.lightweightCharts)
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

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.sqliteDriver)
            implementation(libs.sqldelight.primitiveAdapters)
            implementation(libs.sqldelight.coroutinesExtensions)

            implementation(libs.molecule.runtime)

            // Multiplatform Settings
            implementation(libs.multiplatformSettings.core)
            implementation(libs.multiplatformSettings.coroutines)
            implementation(libs.multiplatformSettings.datastore)

            // kotlin-csv
            implementation(libs.kotlinCsvJvm)

            // JavaFx
            implementation("org.openjfx:javafx-base:23:linux")
            implementation("org.openjfx:javafx-controls:23:linux")
            implementation("org.openjfx:javafx-graphics:23:linux")
            implementation("org.openjfx:javafx-media:23:linux")
            implementation("org.openjfx:javafx-swing:23:linux")
            implementation("org.openjfx:javafx-web:23:linux")

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

            // Jetpack Datastore
            implementation(libs.jetpack.datastore.preferences)

            // Clikt
            implementation("com.github.ajalt.clikt:clikt:5.0.1")

            // godaddy / compose-color-picker
            implementation("com.godaddy.android.colorpicker:compose-color-picker:0.7.0")
        }

        jvmTest.dependencies {

            implementation(kotlin("test"))

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.test)

            // Jimfs
            implementation("com.google.jimfs:jimfs:1.3.0")
        }
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
    stabilityConfigurationFile = parent!!.layout.projectDirectory.file("compose-stability.conf")

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
        )

        if (findProperty("debugMode") == "true") {
            args("--debug")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TradingCompanion"
            packageVersion = "1.0.0"
        }
    }
}
