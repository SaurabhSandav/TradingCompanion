import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.jetbrains.compose)
    id("de.undercouch.download")
}

kotlin {

    jvmToolchain(17)

    jvm {

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    compilerOptions {

        progressiveMode = true

        freeCompilerArgs.addAll(
            "-Xwhen-guards",
        )

        optIn = listOf(
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.ExperimentalStdlibApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {

        jvmMain.dependencies {

            // Compose
            implementation(compose.ui)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // Kermit
            implementation(libs.kermit)

            api("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.839")
        }

        jvmTest.dependencies {

            implementation(kotlin("test"))
        }
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

val downloadLWC by tasks.registering(Download::class) {
    src("https://unpkg.com/lightweight-charts@4.2.1/dist/lightweight-charts.standalone.production.js")
    dest("src/jvmMain/resources/charts_page")
    overwrite(false)
}

tasks.withType<ProcessResources> {
    dependsOn(downloadLWC)
}
