package com.saurabhsandav.core.ui.tags.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.tags.form.TagFormDialog
import com.saurabhsandav.core.ui.tags.form.TagFormType
import com.saurabhsandav.core.ui.tags.model.TagsState.Tag

@Composable
internal fun TagListItem(
    tag: Tag,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {

    var showEditDialog by state { false }
    var showDeleteConfirmationDialog by state { false }

    ListItem(
        headlineContent = { Text(tag.name) },
        supportingContent = { Text(tag.description) },
        trailingContent = {

            Row {

                IconButton(onClick = onCopy) {

                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }

                IconButton(onClick = { showEditDialog = true }) {

                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }

                IconButton(onClick = { showDeleteConfirmationDialog = true }) {

                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        },
    )

    if (showEditDialog) {

        TagFormDialog(
            profileId = tag.id.profileId,
            type = remember { TagFormType.Edit(tag.id.tagId) },
            onCloseRequest = { showEditDialog = false },
        )
    }

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        text = {
            Text("Are you sure you want to delete the tag?")
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
    )
}
