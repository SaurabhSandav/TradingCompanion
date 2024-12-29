import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildKonfig)
}

group = "com.saurabhsandav.apps"
version = "1.0-SNAPSHOT"

configurations.configureEach {

    // App doesn't use Material. Don't want suggestions in autocomplete.
    exclude("org.jetbrains.compose.material", "material")
}

kotlin {

    jvm {

        compilerOptions.freeCompilerArgs.add("-Xjdk-release=21")

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    compilerOptions {

        progressiveMode = true

        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
            "-Xcontext-receivers",
            "-Xconsistent-data-class-copy-visibility",
            "-Xwhen-guards",
        )

        optIn = listOf(
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
        )
    }

    sourceSets {

        commonMain.dependencies {

            implementation(projects.fyersApi)
            implementation(projects.lightweightCharts)
        }

        jvmMain.dependencies {

            implementation(compose.desktop.currentOs)
            implementation(compose.components.resources)
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

            // kotlin-result
            implementation(libs.kotlinResult)
            implementation(libs.kotlinResult.coroutines)

            // AppDirs
            implementation(libs.appdirs)

            // Kermit
            implementation(libs.kermit)

            // FileKit
            implementation(libs.filekit.core)

            // compose-richtext
            implementation(libs.composeRichtext.commonmark)
            implementation(libs.composeRichtext.ui.material3)

            // Jetpack Paging
            implementation(libs.jetpack.paging.common)

            // Jetpack Datastore
            implementation(libs.jetpack.datastore.preferences)

            // Clikt
            implementation(libs.clikt)

            // ColorPicker Compose
            implementation(libs.colorpickerCompose)

            // Apache Commons Compress
            implementation(libs.apacheCommons.compress)

            // JCEF Compose
            implementation(projects.jcefCompose)
        }

        jvmTest.dependencies {

            implementation(kotlin("test"))

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.test)

            // Jimfs
            implementation(libs.jimfs)
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

buildkonfig {
    packageName = "com.saurabhsandav.core"

    defaultConfigs {

        buildConfigField(
            type = FieldSpec.Type.BOOLEAN,
            name = "DEBUG_MODE",
            value = (findProperty("debugMode")?.toString()?.lowercase() == "true").toString(),
        )
    }
}

composeCompiler {

    stabilityConfigurationFiles.addAll(
        parent!!.layout.projectDirectory.file("compose-stability.conf")
    )

    // Trigger this with:
    // ./gradlew build -PenableComposeCompilerReports --rerun-tasks
    if (project.providers.gradleProperty("enableComposeCompilerReports").isPresent) {
        val composeReports = layout.buildDirectory.map { it.dir("reports").dir("compose") }
        reportsDestination.set(composeReports)
        metricsDestination.set(composeReports)
    }
}

compose {

    resources {
        generateResClass = always
    }

    desktop {
        application {

            mainClass = "com.saurabhsandav.core.MainKt"

            jvmArgs += listOf(
                "--add-opens=java.desktop/java.awt.peer=ALL-UNNAMED",
                "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                // For setting WM_CLASS
                "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
            )

            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
                packageName = "TradingCompanion"
                packageVersion = "1.0.0"

                modules(
                    // SQL
                    "java.sql",
                    // Datastore
                    "jdk.unsupported",
                    // JCEF
                    "jcef",
                    // FileKit
                    "jdk.security.auth",
                )

                linux {
                    iconFile.set(project.file("src/jvmMain/composeResources/drawable/icon.png"))
                }
            }
        }
    }
}

// Fix jcef_helper file is not executable in Jetbrains JCEF
val fixDistributablePermissions by tasks.registering(Exec::class) {
    workingDir(layout.buildDirectory.file("compose/binaries/main/app/TradingCompanion/lib/runtime/lib/"))
    commandLine("chmod", "+x", "jcef_helper")
}

tasks.withType<AbstractJPackageTask> {
    finalizedBy(fixDistributablePermissions)
}
