package com.saurabhsandav.core.ui.settings.backup.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEachIndexed
import com.saurabhsandav.core.backup.service.BackupService
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.app.AppDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.ConfiguredService
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.Service
import com.saurabhsandav.core.ui.settings.backup.model.BackupSettingsState.ServiceType
import com.saurabhsandav.core.ui.settings.ui.PreferenceHeader
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun ConfiguredServices(
    modifier: Modifier,
    configuredServices: List<ConfiguredService>,
    onEdit: (BackupService.Id) -> Unit,
    onDelete: (BackupService.Id) -> Unit,
    onBackup: (BackupService.Id) -> Unit,
) {

    Column(modifier) {

        PreferenceHeader(
            headlineContent = { Text("Configured Services") },
        )

        HorizontalDivider()

        when {
            configuredServices.isEmpty() -> {

                Text(
                    modifier = Modifier.fillMaxSize().wrapContentSize(),
                    text = "No Services Configured",
                )
            }

            else -> configuredServices.forEachIndexed { index, service ->

                key(service) {

                    ConfiguredServiceListItem(
                        name = service.name,
                        service = service.service,
                        onEdit = { onEdit(service.id) },
                        onBackup = { onBackup(service.id) },
                        onDelete = { onDelete(service.id) },
                    )

                    if (index != configuredServices.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ConfiguredServiceListItem(
    name: String,
    service: String,
    onEdit: () -> Unit,
    onBackup: () -> Unit,
    onDelete: () -> Unit,
) {

    var showDeleteConfirmationDialog by state { false }

    ListItem(
        modifier = Modifier.clickable(onClick = onEdit),
        headlineContent = { Text(name) },
        overlineContent = { Text(service) },
        trailingContent = {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.dimens.rowHorizontalSpacing,
                    alignment = Alignment.CenterHorizontally,
                ),
            ) {

                IconButtonWithTooltip(
                    onClick = onBackup,
                    tooltipText = "Backup",
                ) {
                    Icon(Icons.Default.Backup, contentDescription = "Backup")
                }

                IconButtonWithTooltip(
                    onClick = { showDeleteConfirmationDialog = true },
                    tooltipText = "Delete",
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        },
    )

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = "service",
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}

@Composable
internal fun NewServiceDialog(
    onDismissRequest: () -> Unit,
    services: List<Service>,
    onSelectService: (ServiceType) -> Unit,
) {

    AppDialog(
        onDismissRequest = onDismissRequest,
        size = MaterialTheme.dimens.dialogSize.copy(height = Dp.Unspecified),
    ) {

        Column {

            services.fastForEachIndexed { index, backupService ->

                key(backupService) {

                    ListItem(
                        modifier = Modifier.clickable(onClick = { onSelectService(backupService.type) }),
                        headlineContent = { Text(backupService.name) },
                        supportingContent = { Text(backupService.description) },
                    )

                    if (index != services.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}
