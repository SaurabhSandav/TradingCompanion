package com.saurabhsandav.core.ui.settings.backup.model

import com.saurabhsandav.core.backup.service.BackupService

internal sealed class BackupSettingsEvent {

    data class Backup(
        val toDirPath: String,
    ) : BackupSettingsEvent()

    data class Restore(
        val archivePath: String,
    ) : BackupSettingsEvent()

    data class DeleteService(
        val id: BackupService.Id,
    ) : BackupSettingsEvent()

    data class BackupToService(
        val id: BackupService.Id,
    ) : BackupSettingsEvent()
}
