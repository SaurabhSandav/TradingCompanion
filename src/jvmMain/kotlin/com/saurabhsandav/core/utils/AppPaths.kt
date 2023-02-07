package com.saurabhsandav.core.utils

import net.harawata.appdirs.AppDirsFactory
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

object AppPaths {

    val appName = "TradingCompanion"

    fun getAppDataPath(): String {

        val appDirs = AppDirsFactory.getInstance()

        val path = appDirs.getUserDataDir(appName, null, "SaurabhSandav")

        Path(path).createDirectories()

        return path
    }
}
