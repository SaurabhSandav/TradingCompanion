package com.saurabhsandav.core.ui.autotrader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.trades.model.AutoTraderScriptId
import com.saurabhsandav.core.ui.autotrader.model.AutoTraderState.Script
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ColumnScope.Scripts(
    scripts: ImmutableList<Script>,
    isSelected: (AutoTraderScriptId) -> Boolean,
    onNewScript: () -> Unit,
    onSelectScript: (AutoTraderScriptId) -> Unit,
    onCopyScript: (AutoTraderScriptId) -> Unit,
    onDeleteScript: (AutoTraderScriptId) -> Unit,
) {

    LazyColumn(Modifier.weight(1F)) {

        items(scripts) { script ->

            Script(
                title = script.title,
                description = script.description,
                isSelected = isSelected(script.id),
                onSelect = { onSelectScript(script.id) },
                onCopy = { onCopyScript(script.id) },
                onDelete = { onDeleteScript(script.id) },
            )
        }
    }

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onNewScript,
        content = { Text("New") },
    )
}

@Composable
private fun Script(
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {

    var showDeleteConfirmationDialog by state { false }

    ListItem(
        modifier = Modifier.clickable(onClick = onSelect),
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = {

            Row {

                IconButtonWithTooltip(
                    onClick = onCopy,
                    tooltipText = "Copy",
                    content = {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    },
                )

                IconButtonWithTooltip(
                    onClick = { showDeleteConfirmationDialog = true },
                    tooltipText = "Delete",
                    content = {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    },
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> ListItemDefaults.containerColor
            },
        ),
    )

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = "script",
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}
