package com.saurabhsandav.core.utils

import net.harawata.appdirs.AppDirsFactory
import java.nio.file.Files
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

    fun createTempDirectory(prefix: String): Path

    companion object {

        operator fun invoke(isDebugMode: Boolean): AppPaths = AppPathsImpl(isDebugMode)
    }
}

private class AppPathsImpl(
    private val isDebugMode: Boolean,
) : AppPaths {

    override val appName = "TradingCompanion"

    override val appDataPath: Path
        get() {

            val appName = if (isDebugMode) "$appName [Debug]" else appName

            val pathStr = AppDirsFactory
                .getInstance()
                .getUserDataDir(appName, null, "SaurabhSandav")

            return Path(pathStr).also { it.createDirectories() }
        }

    override fun createTempDirectory(prefix: String): Path {
        return Files.createTempDirectory(prefix)
    }
}
