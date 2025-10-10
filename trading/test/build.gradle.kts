plugins {
    id("convention.kotlin.multiplatform")
}

group = "com.saurabhsandav.trading"
version = "1.0-SNAPSHOT"

kotlin {

    compilerOptions {

        optIn.addAll(
            "kotlin.time.ExperimentalTime",
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.trading.core)
            api(projects.trading.broker)

            implementation(kotlin("test"))

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.softwork.kotlinxSerializationCsv)
        }
    }
}
