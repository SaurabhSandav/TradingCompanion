plugins {
    id("convention.kotlin.multiplatform")

    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.saurabhsandav.trading"
version = "1.0-SNAPSHOT"

kotlin {

    compilerOptions {

        optIn.addAll(
            "kotlin.time.ExperimentalTime",
        )
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.trading.core)
            api(projects.trading.record)
            api(projects.trading.broker)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // KotlinX Immutable Collections Library
            implementation(libs.kotlinx.collections.immutable)
        }

        commonTest.dependencies {

            implementation(projects.trading.test)
        }
    }
}
