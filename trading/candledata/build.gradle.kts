plugins {
    id("convention.kotlin.multiplatform")

    alias(libs.plugins.sqldelight)
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

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // KotlinX DateTime
            implementation(libs.kotlinx.datetime)

            // kotlin-result
            implementation(libs.kotlinResult)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.sqliteDriver)
            implementation(libs.sqldelight.coroutinesExtensions)
        }
    }
}

sqldelight {
    databases {

        create("CandleDB") {
            packageName = "com.saurabhsandav.trading.candledata"
            srcDirs("src/commonMain/sqldelight/candles")
            schemaOutputDirectory = file("src/commonMain/sqldelight/candles")
            dialect(libs.sqldelight.dialect.sqlite338)
        }
    }
}
