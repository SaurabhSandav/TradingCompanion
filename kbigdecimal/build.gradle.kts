import com.saurabhsandav.buildlogic.convention.applyWebConventions

plugins {
    id("convention.kotlin.multiplatform")

    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {

    applyWebConventions()

    compilerOptions {

        explicitApi()

        freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )

        optIn.addAll(
            "kotlinx.serialization.ExperimentalSerializationApi",
        )
    }

    sourceSets {

        commonMain.dependencies {

            // KotlinX Serialization
            implementation(libs.kotlinx.serialization.core)
        }

        commonTest.dependencies {

            // KotlinX Serialization
            implementation(libs.softwork.kotlinxSerializationCsv)
        }

        webMain.dependencies {

            implementation(npm("bignumber.js", "9.3.1"))
        }
    }
}

val generateData by tasks.registering(JavaExec::class) {
    mainClass.set("com.saurabhsandav.kbigdecimal.TestData_jvmKt")
    classpath = sourceSets["jvmTest"].runtimeClasspath
    description = "Generate Test Data for KBigDecimal"
    args(layout.projectDirectory.file("src/commonTest/kotlin/com/saurabhsandav/kbigdecimal/TestDataCsv.kt"))
}
