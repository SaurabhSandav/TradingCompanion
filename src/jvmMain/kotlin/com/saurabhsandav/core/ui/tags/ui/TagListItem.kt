package com.saurabhsandav.core.ui.tags.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag

@Composable
internal fun TagListItem(
    tag: Tag,
    onNewTag: () -> Unit,
    onEditTag: () -> Unit,
    onDelete: () -> Unit,
) {

    var showDeleteConfirmationDialog by state { false }

    ListItem(
        modifier = Modifier.clickable(onClick = onEditTag),
        headlineContent = { Text(tag.name) },
        supportingContent = tag.description?.let { { Text(it) } },
        trailingContent = {

            Row {

                IconButtonWithTooltip(
                    onClick = onNewTag,
                    tooltipText = "New",
                    content = {
                        Icon(Icons.Default.NewLabel, contentDescription = "New")
                    },
                )

                IconButtonWithTooltip(
                    onClick = onEditTag,
                    tooltipText = "Edit",
                    content = {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
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
    )

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = "tag",
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}
