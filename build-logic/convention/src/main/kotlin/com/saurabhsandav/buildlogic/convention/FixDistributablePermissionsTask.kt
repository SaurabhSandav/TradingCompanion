package com.saurabhsandav.buildlogic.convention

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.withType
import org.gradle.process.ExecOperations
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import javax.inject.Inject

/** Fix jcef_helper file is not executable in Jetbrains JCEF **/
internal abstract class FixDistributablePermissionsTask
    @Inject
    constructor(
        private val execOperations: ExecOperations,
    ) : DefaultTask() {

        @get:InputDirectory
        abstract val workingDirectory: DirectoryProperty

        @get:Input
        abstract val filesToFix: ListProperty<String>

        @TaskAction
        fun fixPermissions() {

            val workingDirFile = workingDirectory.asFile.get()

            val existingFiles = filesToFix.get().filter { fileName ->
                workingDirFile.resolve(fileName).exists()
            }

            if (existingFiles.isEmpty()) {
                logger.warn("[FixDistributablePermissionsTask] No files to fix")
                return
            }

            execOperations.exec {
                workingDir(workingDirectory)
                commandLine("chmod", "+x", "-f", *existingFiles.toTypedArray())
            }

            logger.lifecycle("[FixDistributablePermissionsTask] Fixed jcef_helper at $existingFiles")
        }
    }

internal fun Project.fixAppDistributablePermissions() {

    val fixDistributablePermissions by tasks.registering(FixDistributablePermissionsTask::class) {
        workingDirectory.set(layout.buildDirectory.dir("compose/binaries/"))
        filesToFix.addAll(
            "main/app/TradingCompanion/lib/runtime/lib/jcef_helper",
            "main-release/app/TradingCompanion/lib/runtime/lib/jcef_helper",
        )
    }

    tasks.withType<AbstractJPackageTask>().configureEach {
        finalizedBy(fixDistributablePermissions)
    }
}
