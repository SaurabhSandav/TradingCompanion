import de.undercouch.gradle.tasks.download.Download

plugins {
    id("convention.compose-multiplatform.library")

    alias(libs.plugins.kotlin.plugin.serialization)
    id("de.undercouch.download")
}

kotlin {

    compilerOptions {

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
