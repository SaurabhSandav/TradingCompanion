import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.buildKonfig)
}

kotlin {

    jvmToolchain(17)

    jvm {

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {

        configureEach {

            languageSettings {

                progressiveMode = true
                explicitApi()

                listOf(
                    "kotlin.ExperimentalStdlibApi",
                ).forEach { optIn(it) }
            }
        }

        commonMain.dependencies {

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // KotlinX DateTime
            implementation(libs.kotlinx.datetime)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)

            // Kermit
            implementation(libs.kermit)

            // cryptography-kotlin
            implementation("dev.whyoleg.cryptography:cryptography-core:0.4.0")
            implementation("dev.whyoleg.cryptography:cryptography-provider-jdk:0.4.0")
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
    packageName = "com.saurabhsandav.fyers_api"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "FYERS_APP_ID", FYERS_APP_ID)
        buildConfigField(FieldSpec.Type.STRING, "FYERS_SECRET", FYERS_SECRET)
    }
}
