package com.saurabhsandav.buildlogic.convention

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import java.util.Locale

/**
 * Convention Plugin to automatically apply ResourceGeneratorTask to all KMP source sets.
 */
class TestResourcesConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        // Ensure the Kotlin Multiplatform plugin is applied
        project.plugins.withType(KotlinMultiplatformPluginWrapper::class.java) {

            // 1. Create and configure the extension for user customization
            val extension = project.extensions.create("testResources", TestResourcesPluginExtension::class.java)
            val sourceSets = project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets

            // Iterate over all available source sets (commonMain, androidMain, etc.)
            sourceSets.forEach { sourceSet ->

                if (!sourceSet.name.startsWith("common")) return@forEach // Only target 'common' source sets

                val hasResources = sourceSet.resources.srcDirs.any { it.list()?.isNotEmpty() == true }
                if (!hasResources) return@forEach

                val taskName = "generateResFor${sourceSet.name.capitalize()}"

                // 2. Register the custom task
                val resTask = project.tasks.register(taskName, ResourceGeneratorTask::class.java) {
                    sourceSetName.set(sourceSet.name)
                    // Define the output location under the build directory
                    outputDir.set(project.layout.buildDirectory.dir("generated/res/${sourceSet.name}"))
                    // Use the configured (or default) package name from the extension
                    packageName.set(extension.packageName)
                    // Get the resource directories from the source set's resources
                    resourceDirs.set(sourceSet.resources.srcDirs)
                }

                // 3. Add the generated directory to the Kotlin source set's paths
                sourceSet.kotlin.srcDir(resTask.map { it.outputDir })

                // 4. Make the compilation task depend on the resource generation task (Run before compilation)
                // This ensures the generated Res.kt file exists before the compiler runs.
                project.afterEvaluate {
                    sourceSet.getCompileTaskName("Kotlin").let { compileTaskName ->
                        project.tasks.findByName(compileTaskName)?.dependsOn(resTask)
                    }
                }
            }
        }
    }

    /**
     * Extension function to get the correct compilation task name across different targets.
     * e.g., "compileKotlinJvm", "compileKotlinAndroid", "compileKotlinCommon"
     */
    private fun KotlinSourceSet.getCompileTaskName(language: String): String {
        return project.tasks.names.firstOrNull {
            it.startsWith("compile$language") && it.endsWith(this.name.capitalize())
        } ?: "compile$language${this.name.capitalize()}" // Fallback convention
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

/**
 * Extension class for configuring the TestResourcesConventionPlugin.
 */
abstract class TestResourcesPluginExtension {

    /** The package name where the generated Res object will be created (e.g., "my.app.resources") */
    abstract val packageName: Property<String>
}

/**
 * Custom Gradle Task to scan resources in a KotlinSourceSet and generate a Kotlin object
 * containing their contents as String properties.
 *
 * This allows access to file contents at compile time in a type-safe manner.
 */
abstract class ResourceGeneratorTask : DefaultTask() {

    // The KotlinSourceSet to scan (e.g., commonMain, androidMain)
    @get:Input
    abstract val sourceSetName: Property<String>

    // The package name for the generated Kotlin file (e.g., "com.mykmp.res")
    @get:Input
    abstract val packageName: Property<String>

    // The resource directories of the source set to scan
    @get:InputFiles
    abstract val resourceDirs: SetProperty<File>

    // The directory where the generated Kotlin file will be placed
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        // Ensure the output directory exists and is clean
        val outputDirectory = outputDir.get().asFile
        outputDirectory.mkdirs()
        outputDirectory.listFiles()?.forEach { it.delete() }

        val generatedFile = outputDirectory.resolve("Res.kt")
        val content = generateKotlinFileContent()

        generatedFile.writeText(content)
        logger.info("Generated resource access file: ${generatedFile.path}")
    }

    private fun generateKotlinFileContent(): String {

        val allResources = mutableMapOf<String, String>()

        // 1. Scan all resource directories for target files
        resourceDirs.get().forEach { resourceDir ->
            if (resourceDir.exists() && resourceDir.isDirectory) {
                resourceDir.walkTopDown()
                    .filter { it.isFile }
                    .filter { it.extension == "json" || it.extension == "csv" }
                    .forEach { file ->
                        val variableName = file.nameWithoutExtension
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            .replace(Regex("[^a-zA-Z0-9]")) { it.value.uppercase(Locale.getDefault()) } +
                            file.extension.uppercase()

                        // Check for duplicates across different resource folders of the same source set
                        if (allResources.containsKey(variableName)) {
                            error("Duplicate resource file name detected: ${file.name} in source set ${sourceSetName.get()}")
                        }

                        // Use triple quotes for multiline strings, escaping the triple quotes themselves if they exist in the content
                        val fileContent = file.readText()
                            .replace("\"", "\\\"")

                        allResources[variableName] = fileContent
                    }
            }
        }

        return buildString {

            // 2. Build the Kotlin file content
            appendLine("package ${packageName.get()}")
            appendLine()
            appendLine("// This file is auto-generated by the ResourceGeneratorTask for ${sourceSetName.get()}")
            appendLine("object Res {")
            appendLine()

            if (allResources.isEmpty()) {
                appendLine("    // No .json or .csv resources found in this source set.")
            } else {
                allResources.forEach { (name, content) ->
                    // Generate a property for each resource
                    appendLine("    /** Content of file: ${name.replace(Regex("(Json|Csv)"), "")}.${name.takeLast(3).lowercase()} */")
                    appendLine("    val $name: String = \"\"\"")
                    appendLine(content.prependIndent("        ")) // Indent content for readability
                    appendLine("        \"\"\".trimIndent()")
                    appendLine()
                }
            }

            appendLine("}")
        }
    }
}
