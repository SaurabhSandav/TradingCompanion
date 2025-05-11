package com.saurabhsandav.core.backup.service

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import kotlinx.coroutines.test.runTest
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalBackupServiceTest {

    @Test
    fun `Save Backup`() = runTest {

        val fakeFileSystem = Jimfs.newFileSystem(Configuration.unix())

        // Create dummy archive file
        val archiveName = "backup.zip"
        val archivePath = fakeFileSystem.getPath("/in")
            .createDirectories()
            .resolve(archiveName)
            .createFile()

        val outDir = fakeFileSystem.getPath("/out").createDirectories()
        val service = LocalBackupService(
            id = BackupService.Id("ID"),
            name = "Local (/out)",
            path = outDir.absolutePathString(),
        )
        val instance = service.newInstance()

        val outArchivePath = outDir.resolve(archiveName)
        assertFalse { outArchivePath.exists() }
        instance.saveBackup(archivePath)
        assertTrue { outArchivePath.exists() }
    }
}
