package com.saurabhsandav.core.ui.settings.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Backup
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.BackupToService
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.DeleteService
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsEvent.Restore
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.ConfiguredService
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Progress
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Service
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.ServiceType
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormDialog
import com.saurabhsandav.core.ui.settings.backup.serviceform.BackupServiceFormType
import com.saurabhsandav.core.ui.settings.backup.ui.BackupButton
import com.saurabhsandav.core.ui.settings.backup.ui.BackupProgressDialog
import com.saurabhsandav.core.ui.settings.backup.ui.ConfiguredServices
import com.saurabhsandav.core.ui.settings.backup.ui.NewServiceButton
import com.saurabhsandav.core.ui.settings.backup.ui.RestoreButton
import com.saurabhsandav.core.ui.settings.ui.Preference
import com.saurabhsandav.core.ui.theme.dimens
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun ColumnScope.BackupPreferencesPane(backupSettingsModule: (CoroutineScope) -> BackupSettingsModule) {

    val scope = rememberCoroutineScope()
    val module = remember { backupSettingsModule(scope) }
    val presenter = remember { module.presenter() }
    val state by presenter.state.collectAsState()

    var serviceFormType by state<BackupServiceFormType?> { null }

    BackupPreferences(
        progress = state.progress,
        onBackup = { toDirPath -> state.eventSink(Backup(toDirPath)) },
        onRestore = { archivePath -> state.eventSink(Restore(archivePath)) },
        services = state.services,
        configuredServices = state.configuredServices,
        onDeleteService = { id -> state.eventSink(DeleteService(id)) },
        onBackupToService = { id -> state.eventSink(BackupToService(id)) },
        onShowServiceForm = { formType -> serviceFormType = formType },
    )

    serviceFormType?.let { formType ->

        val onDismissRequest = { serviceFormType = null }
        val presenter = remember { module.serviceFormPresenter(onDismissRequest, formType) }

        BackupServiceFormDialog(
            onDismissRequest = onDismissRequest,
            formType = formType,
            presenter = presenter,
        )
    }
}

@Composable
private fun ColumnScope.BackupPreferences(
    progress: Progress?,
    onBackup: (toDirPath: String) -> Unit,
    onRestore: (archivePath: String) -> Unit,
    services: List<Service>,
    configuredServices: List<ConfiguredService>,
    onDeleteService: (BackupService.Id) -> Unit,
    onBackupToService: (BackupService.Id) -> Unit,
    onShowServiceForm: (formType: BackupServiceFormType) -> Unit,
) {

    MainOptions(
        onBackup = onBackup,
        onRestore = onRestore,
        services = services,
        onNewService = { type -> onShowServiceForm(BackupServiceFormType.New(type.type)) },
    )

    HorizontalDivider()

    ConfiguredServices(
        modifier = Modifier.weight(1F),
        configuredServices = configuredServices,
        onEdit = { id -> onShowServiceForm(BackupServiceFormType.Edit(id)) },
        onDelete = onDeleteService,
        onBackup = onBackupToService,
    )

    if (progress != null) {
        BackupProgressDialog(progress)
    }
}

@Composable
private fun MainOptions(
    onBackup: (toDirPath: String) -> Unit,
    onRestore: (archivePath: String) -> Unit,
    services: List<Service>,
    onNewService: (ServiceType) -> Unit,
) {

    Preference(
        headlineContent = {

            Row(
                modifier = Modifier.fillMaxWidth().wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            ) {

                BackupButton(
                    onBackup = onBackup,
                )

                RestoreButton(
                    onRestore = onRestore,
                )

                NewServiceButton(
                    services = services,
                    onSelectService = onNewService,
                )
            }
        },
    )
}
