plugins {
    id("convention.compose-multiplatform.library")
}

group = "com.saurabhsandav.trading"
version = "1.0-SNAPSHOT"

kotlin {

    sourceSets {

        commonMain.dependencies {

            // KotlinX Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Jetpack Paging
            implementation(libs.jetpack.paging.common)

            // SQLDelight
            implementation(libs.sqldelight.runtime)

            // Compose
            implementation(compose.runtime)
        }
    }
}
