package com.saurabhsandav.core.ui.settings.backup.model

internal sealed class BackupSettingsEvent {

    data class Backup(
        val toDirPath: String,
    ) : BackupSettingsEvent()

    data class Restore(
        val archivePath: String,
    ) : BackupSettingsEvent()
}
