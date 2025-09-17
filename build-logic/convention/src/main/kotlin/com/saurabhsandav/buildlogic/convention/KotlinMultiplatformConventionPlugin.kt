package com.saurabhsandav.buildlogic.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {

        with(pluginManager) {
            apply(libs.findPlugin("kotlin-multiplatform").get().get().pluginId)
        }

        extensions.configure<KotlinMultiplatformExtension> {
            applyJvmConventions()
            applyCommonConventions()
        }
    }
}

fun KotlinMultiplatformExtension.applyJvmConventions() {

    jvm {

        compilerOptions {
            freeCompilerArgs.add("-Xjdk-release=21")
        }

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
}

fun KotlinMultiplatformExtension.applyJsConventions() {

    js {
        browser()
        compilerOptions {
            this.target.set("es2015")
        }
    }

    project.afterEvaluate {
        disableJsToolingDownload()
    }
}

fun KotlinMultiplatformExtension.applyWasmJsConventions() {

    wasmJs {
        browser()
    }

    project.afterEvaluate {
        disableWasmJsToolingDownload()
    }
}

fun KotlinMultiplatformExtension.applyWebConventions() {

    applyJsConventions()
    applyWasmJsConventions()

    applyDefaultHierarchyTemplate()
}

fun KotlinMultiplatformExtension.applyCommonConventions() {

    compilerOptions {

        progressiveMode.set(true)

        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xconsistent-data-class-copy-visibility",
            "-Xwhen-guards",
            "-Xannotation-default-target=param-property",
        )
    }

    sourceSets.apply {

        commonTest.dependencies {

            implementation(kotlin("test"))

            // KotlinX Coroutines
            implementation(project.libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
