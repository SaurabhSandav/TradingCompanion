package com.saurabhsandav.core.ui.settings.backup.serviceform.service

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.backup.service.RcloneBackupService
import com.saurabhsandav.core.ui.common.form.adapter.addMutableStateField
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceBuilder
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormModel
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType.Edit
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType.New
import com.saurabhsandav.core.ui.settings.backup.serviceform.generateBackupServiceId

class RcloneBackupServiceBuilder(
    private val type: BackupServiceFormType,
) : BackupServiceBuilder {

    private var _formModel by mutableStateOf<RcloneBackupServiceFormModel?>(null)
    override val formModel: BackupServiceFormModel?
        get() = _formModel

    override fun init(service: BackupService?) {

        _formModel = when (service as RcloneBackupService?) {
            null -> RcloneBackupServiceFormModel()
            else -> RcloneBackupServiceFormModel(
                name = service.name,
                remote = service.remote,
                destinationPath = service.destinationPath,
            )
        }
    }

    @Composable
    override fun ColumnScope.Form() {

        val formModel = _formModel ?: return

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = formModel.remoteField.value,
            onValueChange = { value -> formModel.remoteField.holder.value = value },
            label = { Text("Remote") },
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = formModel.destinationPathField.value,
            onValueChange = { value -> formModel.destinationPathField.holder.value = value },
            label = { Text("Destination Path") },
        )
    }

    override fun build(): RcloneBackupService {

        val formModel = _formModel!!

        return RcloneBackupService(
            id = when (type) {
                is New -> generateBackupServiceId()
                is Edit -> type.id
            },
            name = formModel.nameField.value,
            remote = formModel.remoteField.value,
            destinationPath = formModel.destinationPathField.value,
        )
    }

    private class RcloneBackupServiceFormModel(
        name: String = "",
        remote: String = "",
        destinationPath: String = "",
    ) : BackupServiceFormModel(name) {

        val remoteField = addMutableStateField(remote)

        val destinationPathField = addMutableStateField(destinationPath)
    }
}
