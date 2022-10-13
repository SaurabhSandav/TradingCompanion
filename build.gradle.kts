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
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.molecule)
    alias(libs.plugins.sqldelight)
}

group = "com.saurabhsandav.apps"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {

    jvm {

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }

        compilations.all {
            kotlinOptions.jvmTarget = "18"
        }
    }

    sourceSets {

        all {

            languageSettings {

                progressiveMode = true

                listOf(
                    "androidx.compose.foundation.ExperimentalFoundationApi",
                    "androidx.compose.material.ExperimentalMaterialApi",
                    "androidx.compose.material3.ExperimentalMaterial3Api",
                    "androidx.compose.ui.ExperimentalComposeUiApi",
                ).forEach { optIn(it) }
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.material3)

                // KotlinX Coroutines
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.swing)

                // KotlinX DateTime
                implementation(libs.kotlinx.datetime)

                // SQLDelight
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.sqliteDriver)
                implementation(libs.sqldelight.coroutinesExtensions)

                // kotlin-csv
                implementation(libs.kotlinCsvJvm)

                // JavaFx
                implementation("org.openjfx:javafx-base:18.0.2:linux")
                implementation("org.openjfx:javafx-controls:18.0.2:linux")
                implementation("org.openjfx:javafx-graphics:18.0.2:linux")
                implementation("org.openjfx:javafx-media:18.0.2:linux")
                implementation("org.openjfx:javafx-swing:18.0.2:linux")
                implementation("org.openjfx:javafx-web:18.0.2:linux")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

sqldelight {

    database("AppDB") {
        packageName = "com.saurabhsandav.core"
        sourceFolders = listOf("sqldelight")
        schemaOutputDirectory = file("build/dbs")
        dialect = "sqlite:3.25"
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TradingCompanion"
            packageVersion = "1.0.0"
        }
    }
}
