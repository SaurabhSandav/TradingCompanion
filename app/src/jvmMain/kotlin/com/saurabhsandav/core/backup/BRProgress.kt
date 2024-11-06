package com.saurabhsandav.core.backup

sealed class BackupEvent {

    data class GeneratingArchive(
        val item: BackupItem?,
        val copied: Long,
        val size: Long,
    ) : BackupEvent()

    data object SavingArchive : BackupEvent()

    data object Finished : BackupEvent()
}

sealed class RestoreEvent {

    data class ExtractingArchive(
        val item: BackupItem?,
        val copied: Long,
        val size: Long,
    ) : RestoreEvent()

    data object ReplacingAppFiles : RestoreEvent()

    data object Finished : RestoreEvent()
}
