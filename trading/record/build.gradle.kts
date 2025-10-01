import com.saurabhsandav.buildlogic.convention.applyWebConventions

plugins {
    id("convention.kotlin.multiplatform")

    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.sqldelight)
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

            api(projects.paging)
            api(projects.trading.core)
            api(projects.trading.broker)

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)

            // KotlinX DateTime
            implementation(libs.kotlinx.datetime)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.primitiveAdapters)
            implementation(libs.sqldelight.coroutinesExtensions)

            // Jetpack Paging
            implementation(libs.jetpack.paging.common)

            // cryptography-kotlin
            implementation(libs.cryptography.core)
        }

        commonTest.dependencies {

            implementation(projects.trading.test)
        }

        jvmMain.dependencies {

            // SQLDelight
            implementation(libs.sqldelight.sqliteDriver)

            // Apache Tika
            implementation(libs.apache.tika.core)

            // cryptography-kotlin
            implementation(libs.cryptography.provider.jdk)
        }

        webMain.dependencies {

            // cryptography-kotlin
            implementation(libs.cryptography.provider.webcrypto)
        }
    }
}

sqldelight {
    databases {

        create("TradesDB") {
            packageName = "com.saurabhsandav.trading.record"
            srcDirs("src/commonMain/sqldelight/trades")
            schemaOutputDirectory = file("src/commonMain/sqldelight/trades")
            dialect(libs.sqldelight.dialect.sqlite338)
        }
    }
}
