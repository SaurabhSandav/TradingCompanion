import com.saurabhsandav.buildlogic.convention.applyWebConventions

plugins {
    id("convention.kotlin.multiplatform")
}

group = "com.saurabhsandav.trading"
version = "1.0-SNAPSHOT"

kotlin {

    applyWebConventions()

    compilerOptions {

        optIn.addAll(
            "kotlin.time.ExperimentalTime",
        )
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.trading.core)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {

            implementation(projects.trading.test)
        }
    }
}
