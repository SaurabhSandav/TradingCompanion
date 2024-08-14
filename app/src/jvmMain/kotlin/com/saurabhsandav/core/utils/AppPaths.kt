package com.saurabhsandav.core.utils

import net.harawata.appdirs.AppDirsFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

object AppPaths {

    val appName = "TradingCompanion"

    val appDataPath: Path
        get() {

            val pathStr = AppDirsFactory
                .getInstance()
                .getUserDataDir(appName, null, "SaurabhSandav")

            return Path(pathStr).also { it.createDirectories() }
        }
}
