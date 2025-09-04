plugins {
    id("convention.kotlin.multiplatform")
}

group = "com.saurabhsandav.trading"
version = "1.0-SNAPSHOT"

kotlin {

    sourceSets {

        commonMain.dependencies {

            api(projects.trading.core)
        }
    }
}
