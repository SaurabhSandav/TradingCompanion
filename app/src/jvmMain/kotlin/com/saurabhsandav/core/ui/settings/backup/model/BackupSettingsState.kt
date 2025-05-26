package com.saurabhsandav.core.ui.settings.backup.model

import com.saurabhsandav.core.backup.BackupItem

internal data class BackupSettingsState(
    val progress: Progress?,
    val eventSink: (BackupSettingsEvent) -> Unit,
) {

    sealed class Progress {

        data class GeneratingArchive(
            val item: BackupItem,
            val progress: Float,
        ) : Progress()

        data object SavingArchive : Progress()
    }
}
