package com.saurabhsandav.core.backup

import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.AppPaths
import com.saurabhsandav.core.utils.ZipUtils
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.nio.file.Path
import kotlin.io.path.*

class BackupManager(
    private val appPaths: AppPaths,
    private val appDispatchers: AppDispatchers,
) {

    suspend fun backup(
        outDir: Path,
        items: Set<BackupItems> = BackupItems.entries.toSet(),
        onProgress: ((BackupEvent) -> Unit)? = null,
    ) = withContext(appDispatchers.IO) {

        if (items.isEmpty()) return@withContext null

        // Notify progress
        onProgress?.invoke(BackupEvent.GeneratingArchive)

        // Create temp dir
        val tempDir = appPaths.createTempDirectory("${appPaths.appName}_Backup")

        // Path of backup archive
        val timestamp = Clock.System.now()
        val archivePath = tempDir.resolve("TC_backup_$timestamp.zip")

        // Get paths for specified backup items.
        val paths = items.map { item ->

            when (item) {
                BackupItems.Prefs -> appPaths.prefsPath
                BackupItems.AppDb -> appPaths.appDBPath
                BackupItems.TradingRecords -> appPaths.tradingRecordsPath
                BackupItems.Candles -> appPaths.candlesDBPath
            }
        }

        // Create backup archive
        ZipUtils.createZip(
            paths = paths,
            outPath = archivePath,
        )

        // Notify progress
        onProgress?.invoke(BackupEvent.SavingArchive)

        // Perform backup
        archivePath.copyTo(outDir.resolve(archivePath.fileName))

        // Delete temp dir
        tempDir.deleteRecursively()

        // Notify progress
        onProgress?.invoke(BackupEvent.Finished)
    }

    suspend fun restore(
        archivePath: Path,
        onProgress: ((RestoreEvent) -> Unit)? = null,
    ): Unit = withContext(appDispatchers.IO) {

        // Notify progress
        onProgress?.invoke(RestoreEvent.ExtractingArchive)

        // Create temp dir
        val tempDir = appPaths.createTempDirectory("${appPaths.appName}_Restore")

        // Extract archive to temp dir
        ZipUtils.extractZip(
            zipPath = archivePath,
            outDir = tempDir,
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
