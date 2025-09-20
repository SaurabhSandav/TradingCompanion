package com.saurabhsandav.core.ui.settings.backup.serviceform.service

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.backup.service.LocalBackupService
import com.saurabhsandav.core.ui.common.OutlinedTextBox
import com.saurabhsandav.core.ui.common.app.LocalAppWindowState
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceBuilder
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormModel
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType.Edit
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType.New
import com.saurabhsandav.core.ui.settings.backup.serviceform.generateBackupServiceId
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.path

class LocalBackupServiceBuilder(
    private val type: BackupServiceFormType,
) : BackupServiceBuilder {

    private var _formModel by mutableStateOf<LocalBackupServiceFormModel?>(null)
    override val formModel: BackupServiceFormModel?
        get() = _formModel

    override fun init(service: BackupService?) {

        _formModel = when (service as LocalBackupService?) {
            null -> LocalBackupServiceFormModel()
            else -> LocalBackupServiceFormModel(
                name = service.name,
                path = service.path,
            )
        }
    }

    @Composable
    override fun ColumnScope.Form() {

        val formModel = _formModel ?: return

        var showDirectoryPicker by state { false }
        val window = LocalAppWindowState.current

        LaunchedEffect(showDirectoryPicker) {

            if (!showDirectoryPicker) return@LaunchedEffect

            val path = FileKit.openDirectoryPicker(
                title = "Select Attachment",
                dialogSettings = FileKitDialogSettings(parentWindow = window.window),
            )?.path

            if (path != null) formModel.pathField.holder.value = path

            showDirectoryPicker = false
        }

        OutlinedTextBox(
            modifier = Modifier.fillMaxWidth(),
            value = formModel.pathField.value,
            onClick = { showDirectoryPicker = true },
            label = { Text("Path") },
        )
    }

    override fun build(): LocalBackupService {

        val formModel = _formModel!!

        return LocalBackupService(
            id = when (type) {
                is New -> generateBackupServiceId()
                is Edit -> type.id
            },
            name = formModel.nameField.value,
            path = formModel.pathField.value,
        )
    }

    private class LocalBackupServiceFormModel(
        name: String = "",
        path: String = "",
    ) : BackupServiceFormModel(name) {

        val pathField = addMutableStateField(path)
    }
}
