package com.saurabhsandav.core.backup

import com.saurabhsandav.core.di.TestGraph
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.copyTo
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

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)

        val scheduler = RestoreScheduler()
        val backupManager = testGraph.backupManager

        var counter = 0

        scheduler.restoreAndRestartScope {

            scheduler.init(backupManager) {}

            counter++
        }

        // Check `withRestoreScope` lambda was invoked once.
        // When restore is not scheduled, the lambda in executed should be executed only once.
        assertEquals(1, counter)
    }

    @Test
    fun `Restore scheduled`() = runTest {

        val testGraph = createGraphFactory<TestGraph.Factory>().create(this)

        val restoreScheduler = RestoreScheduler()
        val backupDir = testGraph.fileSystem.getPath("/backup").also { it.createDirectories() }
        val backupManager = testGraph.backupManager

        // Create dummy Prefs file
        val prefsContent = "This is a dummy prefs file"
        val prefsPath = testGraph.appPaths.prefsPath.resolve("prefs.txt")
        prefsPath.createFile().writeText(prefsContent)

        // Create backup
        backupManager.backup(setOf(BackupItem.Prefs)) { archivePath ->
            archivePath.copyTo(backupDir.resolve(archivePath.fileName))
        }

        var exited: Boolean
        var counter = 0

        // Perform restore
        restoreScheduler.restoreAndRestartScope {

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
