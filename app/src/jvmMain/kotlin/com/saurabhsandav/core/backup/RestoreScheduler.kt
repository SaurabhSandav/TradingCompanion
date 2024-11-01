package com.saurabhsandav.core.backup

import java.nio.file.Path

class RestoreScheduler {

    private lateinit var backupManager: BackupManager
    private lateinit var onExit: () -> Unit
    private var archivePath: Path? = null

    fun init(
        backupManager: BackupManager,
        onExit: () -> Unit,
    ) {
        this.backupManager = backupManager
        this.onExit = onExit
    }

    fun schedule(archivePath: Path) {
        this.archivePath = archivePath
        onExit()
    }

    suspend fun withRestoreScope(block: suspend () -> Unit) {

        do {

            archivePath?.let { backupManager.restore(it) }
            archivePath = null

            block()

        } while (archivePath != null)
    }
}
