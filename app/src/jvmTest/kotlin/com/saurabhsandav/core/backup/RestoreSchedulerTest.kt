package com.saurabhsandav.core.backup

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.saurabhsandav.core.FakeAppDispatchers
import com.saurabhsandav.core.FakeAppPaths
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestoreSchedulerTest {

    @Test
    fun `No restore scheduled`() = runTest {

        val scheduler = RestoreScheduler()
        val appDispatchers = FakeAppDispatchers(this)
        val backupManager = BackupManager(
            appPaths = FakeAppPaths(Jimfs.newFileSystem(Configuration.unix())),
            appDispatchers = appDispatchers,
        )

        var counter = 0

        scheduler.withRestoreScope {

            scheduler.init(backupManager) {}

            counter++
        }

        // Check `withRestoreScope` lambda was invoked once.
        // When restore is not scheduled, the lambda in executed should be executed only once.
        assertEquals(1, counter)
    }

    @Test
    fun `Restore scheduled`() = runTest {

        val restoreScheduler = RestoreScheduler()
        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())
        val backupDir = fakeFileSystem.getPath("/backup").also { it.createDirectories() }
        val appPaths = FakeAppPaths(fakeFileSystem)
        val appDispatchers = FakeAppDispatchers(this)
        val backupManager = BackupManager(
            appPaths = appPaths,
            appDispatchers = appDispatchers,
        )

        // Create dummy Prefs file
        val prefsContent = "This is a dummy prefs file"
        val prefsPath = appPaths.prefsPath.resolve("prefs.txt")
        prefsPath.createFile().writeText(prefsContent)

        // Create backup
        backupManager.backup(backupDir, setOf(BackupItems.Prefs))

        var exited: Boolean
        var counter = 0

        // Perform restore
        restoreScheduler.withRestoreScope {

            exited = false

            // Init scheduler
            restoreScheduler.init(backupManager) { exited = true }

            assertFalse { exited }

            if (counter == 0) {
                restoreScheduler.schedule(Files.list(backupDir).findFirst().get())
                assertTrue { exited }
            }

            counter++
        }

        // Check `withRestoreScope` lambda was invoked twice.
        // When restore is scheduled, the lambda should be executed again after performing restore.
        assertEquals(2, counter)
    }
}
