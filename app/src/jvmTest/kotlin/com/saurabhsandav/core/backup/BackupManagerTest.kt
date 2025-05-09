package com.saurabhsandav.core.backup

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.saurabhsandav.core.FakeAppDispatchers
import com.saurabhsandav.core.FakeAppPaths
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BackupManagerTest {

    @Test
    fun backupAndRestore() = runTest {

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

        val backupEvents = mutableListOf<BackupEvent>()

        // Create backup
        backupManager.backup(backupDir, setOf(BackupItem.Prefs)) { event ->
            backupEvents += event
        }

        // Check backup file was created
        assertEquals(Files.list(backupDir).count(), 1)

        // Check backup events
        assertEquals(
            expected = listOf(BackupEvent.GeneratingArchive, BackupEvent.SavingArchive, BackupEvent.Finished),
            actual = backupEvents,
        )

        // Check current data
        assertEquals(prefsContent, prefsPath.readText())

        // Modify data and check
        prefsPath.writeText("This is a modified dummy prefs file")
        assertNotEquals(prefsContent, prefsPath.readText())

        val restoreEvents = mutableListOf<RestoreEvent>()

        // Perform restore and check data was reset
        backupManager.restore(Files.list(backupDir).findFirst().get()) { event ->
            restoreEvents += event
        }
        assertEquals(prefsContent, prefsPath.readText())

        // Check restore events
        assertEquals(
            expected = listOf(RestoreEvent.ExtractingArchive, RestoreEvent.ReplacingAppFiles, RestoreEvent.Finished),
            actual = restoreEvents,
        )
    }
}
