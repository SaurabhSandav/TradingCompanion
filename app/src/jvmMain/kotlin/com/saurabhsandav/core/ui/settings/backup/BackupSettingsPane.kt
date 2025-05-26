package com.saurabhsandav.core.ui.settings.backup

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.saurabhsandav.core.backup.BackupItem
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Backup
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Restore
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress.GeneratingArchive
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress.SavingArchive
import com.saurabhsandav.core.ui.settings.ui.Preference
import com.saurabhsandav.core.ui.theme.dimens
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile
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

                var showDirSelector by state { false }

                Button(
                    onClick = { showDirSelector = true },
                    content = { Text("Backup") },
                )

                LaunchedEffect(showDirSelector) {

                    if (!showDirSelector) return@LaunchedEffect

                    val dir = FileKit.pickDirectory("Backup to")?.path

                    if (dir != null) onBackup(dir)

                    showDirSelector = false
                }

                var showFileSelector by state { false }
                var file by state<String?> { null }

                Button(
                    onClick = { showFileSelector = true },
                    content = { Text("Restore") },
                )

                LaunchedEffect(showFileSelector) {

                    if (!showFileSelector) return@LaunchedEffect

                    file = FileKit.pickFile(
                        type = PickerType.File(listOf("zip")),
                        title = "Restore from",
                    )?.path

                    showFileSelector = false
                }

                val fileL = file
                if (fileL != null) {

                    ConfirmationDialog(
                        text = "Are you sure you want to restore this backup? (App will restart)",
                        onDismiss = { file = null },
                        onConfirm = {
                            onRestore(fileL)
                            file = null
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun BackupProgressDialog(progress: Progress) {

    Dialog(onDismissRequest = {}) {

        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
        ) {

            Column(
                modifier = Modifier
                    .padding(MaterialTheme.dimens.containerPadding)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
            ) {

                when (progress) {
                    is GeneratingArchive -> {

                        Text("Generating Archive")

                        val subtitle = when (progress.item) {
                            BackupItem.Prefs -> "Prefs"
                            BackupItem.AppDb -> "App Database"
                            BackupItem.TradingRecords -> "Trading Records"
                            BackupItem.Candles -> "Candles Database"
                        }

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { progress.progress },
                        )
                    }

                    SavingArchive -> {

                        Text("Saving Archive")

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
