package com.saurabhsandav.core.utils

import com.saurabhsandav.core.di.IsDebugMode
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
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
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class RealAppPaths(
    @IsDebugMode isDebugMode: Boolean,
) : AppPaths {

    override val appName = "TradingCompanion"

    init {
        FileKit.init(appId = if (isDebugMode) "$appName [Debug]" else appName)
    }

    override val appDataPath: Path
        get() = Path(FileKit.filesDir.path).also { it.createDirectories() }

    override fun createTempDirectory(prefix: String): Path {
        return Files.createTempDirectory(prefix)
    }
}
