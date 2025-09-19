import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.saurabhsandav.buildlogic.convention.generateAppVersion
import com.saurabhsandav.buildlogic.convention.isReleaseBuild
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id("convention.compose-multiplatform.app")

    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.metro)
}

group = "com.saurabhsandav.apps"
version = "1.0-SNAPSHOT"

configurations.configureEach {

    // App doesn't use Material. Don't want suggestions in autocomplete.
    exclude("org.jetbrains.compose.material", "material")
}

kotlin {

    jvm {

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {

            mainClass = "com.saurabhsandav.core.MainKt"
        }
    }

    compilerOptions {

        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )

        optIn.addAll(
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
        }

        jvmTest.dependencies {

            implementation(projects.trading.test)

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
            name = "IS_DEBUG_BUILD",
            value = isReleaseBuild.not().toString(),
        )

        buildConfigField(
            type = FieldSpec.Type.LONG,
            name = "BUILD_TIME",
            value = System.currentTimeMillis().toString(),
        )

        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "VERSION",
            value = generateAppVersion(),
        )
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
