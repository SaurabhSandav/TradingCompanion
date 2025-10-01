import com.saurabhsandav.buildlogic.convention.applyWebConventions

plugins {
    id("convention.kotlin.multiplatform")

    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.saurabhsandav.trading"
version = "1.0-SNAPSHOT"

kotlin {

    applyWebConventions()

    compilerOptions {

        optIn.addAll(
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.time.ExperimentalTime",
        )
    }

    sourceSets {

        commonMain.dependencies {

            api(projects.kbigdecimal)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
        }
    }
}
