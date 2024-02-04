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
            "-Xcontext-parameters",
            "-Xconsistent-data-class-copy-visibility",
            "-Xwhen-guards",
        )

        optIn = listOf(
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.io.path.ExperimentalPathApi",
            "kotlin.uuid.ExperimentalUuidApi",
            "kotlin.time.ExperimentalTime",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.serialization.ExperimentalSerializationApi",
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

            implementation(projects.paging)
            implementation(projects.fyersApi)
            implementation(projects.lightweightCharts)
            implementation(projects.trading.core)
            implementation(projects.trading.indicator)
            implementation(projects.trading.barreplay)
            implementation(projects.trading.record)
            implementation(projects.trading.backtest)
            implementation(projects.trading.candledata)
            implementation(projects.trading.market.india)
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

            // kotlin-result
            implementation(libs.kotlinResult)
            implementation(libs.kotlinResult.coroutines)

            // Kermit
            implementation(libs.kermit)

            // FileKit
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)

            // mikepenz/multiplatform-markdown-renderer
            implementation(libs.markdownRenderer.core)
            implementation(libs.markdownRenderer.m3)

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

            // EitherNet
            implementation(libs.eithernet)

            // Coil
            implementation(libs.coil.compose)

            // ZoomImage
            implementation(libs.zoomimage.composeCoil3)

            // Scripting
            implementation("org.jetbrains.kotlin:kotlin-scripting-common:${libs.versions.kotlin.get()}")
            implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:${libs.versions.kotlin.get()}")
            implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:${libs.versions.kotlin.get()}")

            // ktfmt
            implementation("com.facebook:ktfmt:0.47")
        }

        jvmTest.dependencies {

            implementation(kotlin("test"))
            implementation(projects.trading.test)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.test)

            // Multiplatform Settings
            implementation(libs.multiplatformSettings.test)

            // Turbine
            implementation(libs.turbine)

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
        parent!!.layout.projectDirectory.file("compose-stability.conf"),
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

            buildTypes.release.proguard {
                isEnabled.set(false)
            }
        }
    }
}

// Fix jcef_helper file is not executable in Jetbrains JCEF
val fixDistributablePermissions by tasks.registering(Exec::class) {
    workingDir(layout.buildDirectory.file("compose/binaries/"))

    commandLine(
        "chmod",
        "+x",
        "-f",
        "main/app/TradingCompanion/lib/runtime/lib/jcef_helper",
        "main-release/app/TradingCompanion/lib/runtime/lib/jcef_helper",
    )

    isIgnoreExitValue = true
}

tasks.withType<AbstractJPackageTask> {
    finalizedBy(fixDistributablePermissions)
}
