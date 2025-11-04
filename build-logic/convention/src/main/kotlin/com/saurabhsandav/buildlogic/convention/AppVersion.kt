package com.saurabhsandav.buildlogic.convention

import org.gradle.api.Project

fun Project.generateAppVersion(): String {

    return when {
        isPackageBuild -> providers.exec {
            environment("TZ", "UTC0")
            commandLine(
                "git",
                "log",
                "-1",
                "--date=local",
                "--pretty=format:%cd.%h",
                "--date=format-local:%Y%m%d.%H%M%S",
            )
        }.standardOutput.asText.get().trim().plus(if (isReleaseBuild) ".release" else ".debug")

        else -> "DEBUG"
    }
}
