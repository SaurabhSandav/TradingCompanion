package com.saurabhsandav.core.ui.settings.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.saurabhsandav.core.backup.BackupEvent
import com.saurabhsandav.core.backup.BackupManager
import com.saurabhsandav.core.backup.RestoreScheduler
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Backup
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Restore
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CoroutineScope
import kotlin.io.path.Path
import kotlin.io.path.copyTo

internal class BackupSettingsPresenter(
    private val coroutineScope: CoroutineScope,
    private val backupManager: BackupManager,
    private val restoreScheduler: RestoreScheduler,
) {

    private var progress by mutableStateOf<Progress?>(null)

    val state = coroutineScope.launchMolecule(RecompositionMode.ContextClock) {

        return@launchMolecule BackupSettingsState(
            progress = progress,
            eventSink = ::onEvent,
        )
    }

    private fun onEvent(event: BackupSettingsEvent) {

        when (event) {
            is Backup -> onBackup(event.toDirPath)
            is Restore -> onRestore(event.archivePath)
        }
    }

    private fun onBackup(toDirPath: String) = coroutineScope.launchUnit {

        backupManager.backup(
            onProgress = { event ->

                progress = when (event) {
                    is BackupEvent.GeneratingArchive -> Progress.GeneratingArchive(
                        item = event.item ?: return@backup,
                        progress = event.copied / event.size.toFloat(),
                    )

                    BackupEvent.SavingArchive -> Progress.SavingArchive
                    BackupEvent.Finished -> null
                }
            },
        ) { archivePath ->
            archivePath.copyTo(Path(toDirPath).resolve(archivePath.fileName))
        }
    }

    private fun onRestore(archivePath: String) {
        restoreScheduler.schedule(Path(archivePath))
    }
}
