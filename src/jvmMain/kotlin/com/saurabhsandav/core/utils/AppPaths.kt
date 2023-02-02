package com.saurabhsandav.core.utils

import kotlin.io.path.Path
import kotlin.io.path.createDirectories

object AppPaths {

    val appName = "TradingCompanion"

    fun getAppDataPath(): String {

        val path = System.getProperty("user.home") + "/.local/share/$appName"

        Path(path).createDirectories()

        return path
    }
}
