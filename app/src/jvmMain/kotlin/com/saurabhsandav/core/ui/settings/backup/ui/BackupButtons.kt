package com.saurabhsandav.core.ui.settings.backup.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.common.ConfirmationDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Service
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.pickFile

@Composable
internal fun BackupButton(onBackup: (toDirPath: String) -> Unit) {

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
}

@Composable
internal fun RestoreButton(onRestore: (archivePath: String) -> Unit) {

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

@Composable
internal fun NewServiceButton(services: List<Service>) {

    var showNewServiceDialog by state { false }

    Button(
        onClick = { showNewServiceDialog = true },
        content = { Text("Add Service") },
    )

    if (showNewServiceDialog) {

        NewServiceDialog(
            onDismissRequest = { showNewServiceDialog = false },
            services = services,
            onSelectService = { type ->
                showNewServiceDialog = false
            },
        )
    }
}
