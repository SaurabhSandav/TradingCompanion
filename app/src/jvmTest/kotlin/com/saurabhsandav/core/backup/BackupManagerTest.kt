package com.saurabhsandav.core.backup

import app.cash.turbine.test
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.saurabhsandav.core.FakeAppDispatchers
import com.saurabhsandav.core.FakeAppPaths
import com.saurabhsandav.core.backup.BackupEvent.GeneratingArchive
import com.saurabhsandav.core.backup.BackupEvent.SavingArchive
import com.saurabhsandav.core.backup.RestoreEvent.ExtractingArchive
import com.saurabhsandav.core.backup.RestoreEvent.ReplacingAppFiles
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BackupManagerTest {

    @Test
    fun `Backup and Restore`() = runTest {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())
        val backupDir = fakeFileSystem.getPath("/backup").also { it.createDirectories() }
        val appPaths = FakeAppPaths(fakeFileSystem)
        val backupManager = BackupManager(
            appPaths = appPaths,
            appDispatchers = FakeAppDispatchers(this),
        )

        // Create dummy Prefs file
        val prefsContent = "This is a dummy prefs file"
        val prefsPath = appPaths.prefsPath.resolve("prefs.txt")
        prefsPath.createFile().writeText(prefsContent)

        // Check no data backed up
        assertEquals(Files.list(backupDir).count(), 0)

        // Create backup
        backupManager.backup(setOf(BackupItem.Prefs)) { archivePath ->
            archivePath.copyTo(backupDir.resolve(archivePath.fileName))
        }

        // Check backup file was created
        assertEquals(Files.list(backupDir).count(), 1)

        // Check current data
        assertEquals(prefsContent, prefsPath.readText())

        // Modify data and check
        prefsPath.writeText("This is a modified dummy prefs file")
        assertNotEquals(prefsContent, prefsPath.readText())

        // Perform restore and check data was reset
        backupManager.restore(Files.list(backupDir).findFirst().get())
        assertEquals(prefsContent, prefsPath.readText())
    }

    @Test
    fun `Backup progress`() = runTest {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())
        val backupDir = fakeFileSystem.getPath("/backup").also { it.createDirectories() }
        val appPaths = FakeAppPaths(fakeFileSystem)
        val backupManager = BackupManager(
            appPaths = appPaths,
            appDispatchers = FakeAppDispatchers(this),
        )

        // Create dummy Prefs file
        val prefsContent = "This is a dummy prefs file"
        val prefsPath = appPaths.prefsPath.resolve("prefs.txt")
        prefsPath.createFile().writeText(prefsContent)

        val channel = Channel<BackupEvent>(Channel.BUFFERED)

        // Create backup
        backupManager.backup(
            items = setOf(BackupItem.Prefs),
            onProgress = channel::trySend,
        ) { archivePath ->
            archivePath.copyTo(backupDir.resolve(archivePath.fileName))
        }

        // Check backup events
        channel.consumeAsFlow().test {
            assertEquals(GeneratingArchive(null, 0, 26), awaitItem())
            assertEquals(GeneratingArchive(BackupItem.Prefs, 0, 26), awaitItem())
            assertEquals(GeneratingArchive(BackupItem.Prefs, 0, 26), awaitItem())
            assertEquals(GeneratingArchive(BackupItem.Prefs, 26, 26), awaitItem())
            assertEquals(GeneratingArchive(null, 26, 26), awaitItem())
            assertEquals(SavingArchive, awaitItem())
            assertEquals(BackupEvent.Finished, awaitItem())
        }
    }

    @Test
    fun `Restore progress`() = runTest {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())
        val backupDir = fakeFileSystem.getPath("/backup").also { it.createDirectories() }
        val appPaths = FakeAppPaths(fakeFileSystem)
        val backupManager = BackupManager(
            appPaths = appPaths,
            appDispatchers = FakeAppDispatchers(this),
        )

        // Create dummy Prefs file
        val prefsContent = "This is a dummy prefs file"
        val prefsPath = appPaths.prefsPath.resolve("prefs.txt")
        prefsPath.createFile().writeText(prefsContent)

        // Create backup
        backupManager.backup(setOf(BackupItem.Prefs)) { archivePath ->
            archivePath.copyTo(backupDir.resolve(archivePath.fileName))
        }

        val channel = Channel<RestoreEvent>(Channel.BUFFERED)

        // Perform restore
        backupManager.restore(Files.list(backupDir).findFirst().get(), channel::trySend)

        // Check restore events
        channel.consumeAsFlow().test {
            assertEquals(ExtractingArchive(null, 0, 26), awaitItem())
            assertEquals(ExtractingArchive(BackupItem.Prefs, 0, 26), awaitItem())
            assertEquals(ExtractingArchive(BackupItem.Prefs, 0, 26), awaitItem())
            assertEquals(ExtractingArchive(BackupItem.Prefs, 26, 26), awaitItem())
            assertEquals(ExtractingArchive(null, 26, 26), awaitItem())
            assertEquals(ReplacingAppFiles, awaitItem())
            assertEquals(RestoreEvent.Finished, awaitItem())
        }
    }
}
