import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.buildKonfig)
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

        explicitApi()

        optIn.addAll(
            "kotlin.ExperimentalStdlibApi",
            "kotlin.time.ExperimentalTime",
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {

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
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.jdk)

            // EitherNet
            implementation(libs.eithernet)
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

val isInCI = providers.environmentVariable("CI").isPresent

buildkonfig {
    packageName = "com.saurabhsandav.fyersapi"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "FYERS_APP_ID", if (isInCI) "" else FYERS_APP_ID)
        buildConfigField(FieldSpec.Type.STRING, "FYERS_SECRET", if (isInCI) "" else FYERS_SECRET)
    }
}
