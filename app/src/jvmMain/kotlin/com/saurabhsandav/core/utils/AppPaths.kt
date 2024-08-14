package com.saurabhsandav.core.utils

import net.harawata.appdirs.AppDirsFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

object AppPaths {

    val appName = "TradingCompanion"

    val appDataPath: Path
        get() {

            val debugMode = System.getProperty("debugMode") == "true"
            val appName = if (debugMode) "$appName [Debug]" else appName

            val pathStr = AppDirsFactory
                .getInstance()
                .getUserDataDir(appName, null, "SaurabhSandav")

            return Path(pathStr).also { it.createDirectories() }
        }

    val prefsPath: Path
        get() = appDataPath.resolve("Prefs").also { it.createDirectories() }
}
