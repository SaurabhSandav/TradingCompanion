package com.saurabhsandav.core.utils

import net.harawata.appdirs.AppDirsFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

interface AppPaths {

    val appName: String

    val appDataPath: Path

    val prefsPath: Path
        get() = appDataPath.resolve("Prefs").also { it.createDirectories() }

    val tradingRecordsPath: Path
        get() = appDataPath.resolve("Records").also { it.createDirectories() }

    val appDBPath: Path
        get() = appDataPath.resolve("$appName.db")

    val candlesDBPath: Path
        get() = appDataPath.resolve("Candles.db")

    companion object {

        operator fun invoke(): AppPaths = AppPathsImpl()
    }
}

private class AppPathsImpl : AppPaths {

    override val appName = "TradingCompanion"

    override val appDataPath: Path
        get() {

            val debugMode = System.getProperty("debugMode") == "true"
            val appName = if (debugMode) "$appName [Debug]" else appName

            val pathStr = AppDirsFactory
                .getInstance()
                .getUserDataDir(appName, null, "SaurabhSandav")

            return Path(pathStr).also { it.createDirectories() }
        }
}
