plugins {
    id("convention.kotlin.multiplatform")
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

            implementation(projects.trading.core)
            implementation(projects.trading.broker)
            implementation(projects.trading.record)
            implementation(projects.fyersApi)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // KotlinX DateTime
            implementation(libs.kotlinx.datetime)
        }
    }
}
