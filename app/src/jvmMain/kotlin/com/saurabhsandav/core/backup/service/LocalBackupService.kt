package com.saurabhsandav.core.backup.service

import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.copyTo

@Serializable
data class LocalBackupService(
    override val id: BackupService.Id,
    override val name: String,
    val path: String,
) : BackupService {

    override fun newInstance(): BackupService.Instance = LocalBackupServiceInstance(this)
}

private class LocalBackupServiceInstance(
    private val service: LocalBackupService,
) : BackupService.Instance {

    override suspend fun saveBackup(archivePath: Path) {
        val outPath = archivePath.fileSystem.getPath(service.path).resolve(archivePath.fileName)
        archivePath.copyTo(outPath)
    }
}
