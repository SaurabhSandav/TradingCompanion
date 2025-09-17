import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.jetbrains.compose)
    id("de.undercouch.download")
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
            "-Xwhen-guards",
        )

        optIn.addAll(
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.ExperimentalStdlibApi",
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "kotlinx.coroutines.FlowPreview",
            "kotlinx.serialization.ExperimentalSerializationApi",
            "kotlin.uuid.ExperimentalUuidApi",
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

            // kotlin css
            api(libs.kotlinWrappers.kotlinCss)
        }

        jvmTest.dependencies {

            implementation(kotlin("test"))
        }
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

val downloadLWC by tasks.registering(Download::class) {
    val version = "5.0.8"
    val development = false
    val flavorStr = if (development) "development" else "production"
    src("https://unpkg.com/lightweight-charts@$version/dist/lightweight-charts.standalone.$flavorStr.js")
    dest("src/jvmMain/resources/charts_page/lightweight-charts.standalone.js")
    overwrite(false)
}

tasks.withType<ProcessResources> {
    dependsOn(downloadLWC)
}
