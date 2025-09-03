import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    id("convention.kotlin.multiplatform")

    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.buildKonfig)
}

kotlin {

    compilerOptions {

        explicitApi()

        optIn.addAll(
            "kotlin.ExperimentalStdlibApi",
            "kotlin.time.ExperimentalTime",
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.kbigdecimal)

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
