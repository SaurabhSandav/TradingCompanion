package com.saurabhsandav.core.backup.service

import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@Serializable
data class RcloneBackupService(
    override val id: BackupService.Id,
    override val name: String,
    val remote: String,
    val destinationPath: String,
) : BackupService {

    override fun newInstance(): BackupService.Instance = RcloneBackupServiceInstance(this)
}

private class RcloneBackupServiceInstance(
    private val service: RcloneBackupService,
) : BackupService.Instance {

    override suspend fun saveBackup(archivePath: Path) {

        val destination = with(service) { "$remote:$destinationPath" }

        val process = ProcessBuilder(
            "rclone",
            "copy",
            archivePath.absolutePathString(),
            destination,
        )

        process.start().waitFor()
    }
}
