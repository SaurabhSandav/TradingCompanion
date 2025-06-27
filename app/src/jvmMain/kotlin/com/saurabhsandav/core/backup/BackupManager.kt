package com.saurabhsandav.core.backup

import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.core.utils.ZipUtils
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyToRecursively
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.Clock

class BackupManager(
    private val appPaths: AppPaths,
    private val appDispatchers: AppDispatchers,
) {

    suspend fun backup(
        items: Set<BackupItem> = BackupItem.entries.toSet(),
        onProgress: ((BackupEvent) -> Unit)? = null,
        onSaveArchive: suspend (Path) -> Unit,
    ): Unit = withContext(appDispatchers.IO) {

        if (items.isEmpty()) return@withContext

        // Create temp dir
        val tempDir = appPaths.createTempDirectory("${appPaths.appName}_Backup")

        // Path of backup archive
        val timestamp = Clock.System.now()
        val archivePath = tempDir.resolve("TC_backup_$timestamp.zip")

        // Get paths for specified backup items.
        val paths = items.map { item ->

            when (item) {
                BackupItem.Prefs -> appPaths.prefsPath
                BackupItem.AppDb -> appPaths.appDBPath
                BackupItem.TradingRecords -> appPaths.tradingRecordsPath
                BackupItem.Candles -> appPaths.candlesDBPath
            }
        }

        // Create backup archive
        ZipUtils.zip(
            paths = paths,
            outPath = archivePath,
            onProgress = { zipProgress ->

                val path = zipProgress.pathProgress?.path

                val item = when {
                    path == null -> null
                    path.startsWith(appPaths.prefsPath.absolutePathString()) -> BackupItem.Prefs
                    path.startsWith(appPaths.appDBPath.absolutePathString()) -> BackupItem.AppDb
                    path.startsWith(appPaths.tradingRecordsPath.absolutePathString()) -> BackupItem.TradingRecords
                    path.startsWith(appPaths.candlesDBPath.absolutePathString()) -> BackupItem.Candles
                    else -> null
                }

                val event = BackupEvent.GeneratingArchive(
                    item = item,
                    copied = zipProgress.copied,
                    size = zipProgress.size,
                )

                onProgress?.invoke(event)
            },
        )

        // Notify progress
        onProgress?.invoke(BackupEvent.SavingArchive)

        // Save backup
        onSaveArchive(archivePath)

        // Delete temp dir
        tempDir.deleteRecursively()

        // Notify progress
        onProgress?.invoke(BackupEvent.Finished)
    }

    suspend fun restore(
        archivePath: Path,
        onProgress: ((RestoreEvent) -> Unit)? = null,
    ): Unit = withContext(appDispatchers.IO) {

        // Create temp dir
        val tempDir = appPaths.createTempDirectory("${appPaths.appName}_Restore")

        // Extract archive to temp dir
        ZipUtils.unzip(
            zipPath = archivePath,
            outDir = tempDir,
            onProgress = { zipProgress ->

                val path = zipProgress.pathProgress?.path

                val item = when {
                    path == null -> null
                    path.startsWith(appPaths.prefsPath.name) -> BackupItem.Prefs
                    path.startsWith(appPaths.appDBPath.name) -> BackupItem.AppDb
                    path.startsWith(appPaths.tradingRecordsPath.name) -> BackupItem.TradingRecords
                    path.startsWith(appPaths.candlesDBPath.name) -> BackupItem.Candles
                    else -> null
                }

                val event = RestoreEvent.ExtractingArchive(
                    item = item,
                    copied = zipProgress.copied,
                    size = zipProgress.size,
                )

                onProgress?.invoke(event)
            },
        )

        // Notify progress
        onProgress?.invoke(RestoreEvent.ReplacingAppFiles)

        // Replace app files with files from archive
        tempDir.listDirectoryEntries().forEach { path ->

            val restorePath = when (path.name) {
                appPaths.prefsPath.name -> appPaths.prefsPath
                appPaths.appDBPath.name -> appPaths.appDBPath
                appPaths.tradingRecordsPath.name -> appPaths.tradingRecordsPath
                appPaths.candlesDBPath.name -> appPaths.candlesDBPath
                else -> return@forEach
            }

            // Delete local files
            restorePath.deleteRecursively()

            // Copy restored files
            path.copyToRecursively(
                target = restorePath,
                followLinks = false,
                overwrite = false,
            )
        }

        // Delete temp dir
        tempDir.deleteRecursively()

        // Notify progress
        onProgress?.invoke(RestoreEvent.Finished)
    }
}
