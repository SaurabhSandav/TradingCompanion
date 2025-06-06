package com.saurabhsandav.core.ui.settings.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Backup
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Restore
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress
import com.saurabhsandav.core.ui.settings.backup.ui.BackupButton
import com.saurabhsandav.core.ui.settings.backup.ui.BackupProgressDialog
import com.saurabhsandav.core.ui.settings.backup.ui.RestoreButton
import com.saurabhsandav.core.ui.settings.ui.Preference
import com.saurabhsandav.core.ui.theme.dimens
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun BackupPreferencesPane(backupSettingsModule: (CoroutineScope) -> BackupSettingsModule) {

    val scope = rememberCoroutineScope()
    val presenter = remember { backupSettingsModule(scope).presenter() }
    val state by presenter.state.collectAsState()

    BackupPreferences(
        progress = state.progress,
        onBackup = { toDirPath -> state.eventSink(Backup(toDirPath)) },
        onRestore = { archivePath -> state.eventSink(Restore(archivePath)) },
    )
}

@Composable
private fun BackupPreferences(
    progress: Progress?,
    onBackup: (toDirPath: String) -> Unit,
    onRestore: (archivePath: String) -> Unit,
) {

    BackupPreference(
        onBackup = onBackup,
        onRestore = onRestore,
    )

    if (progress != null) {
        BackupProgressDialog(progress)
    }
}

@Composable
private fun BackupPreference(
    onBackup: (toDirPath: String) -> Unit,
    onRestore: (archivePath: String) -> Unit,
) {

    Preference(
        headlineContent = { Text("Backup") },
        trailingContent = {

            Row(
                modifier = Modifier.wrapContentSize(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            ) {

                BackupButton(
                    onBackup = onBackup,
                )

                RestoreButton(
                    onRestore = onRestore,
                )
            }
        },
    )
}
