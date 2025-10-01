import com.saurabhsandav.buildlogic.convention.applyWebConventions

plugins {
    id("convention.kotlin.multiplatform")
    id("convention.test.resources")
}

group = "com.saurabhsandav.trading"
version = "1.0-SNAPSHOT"

kotlin {

    applyWebConventions()

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

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.softwork.kotlinxSerializationCsv)
        }
    }
}

testResources {
    packageName = "com.saurabhsandav.trading.test"
}
