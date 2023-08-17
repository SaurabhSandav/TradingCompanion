package com.saurabhsandav.core.ui.profiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.SnackbarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.model.ProfileModel

@Composable
internal fun ProfileListItem(
    name: String,
    description: String,
    isTraining: Boolean,
    isCurrent: Boolean,
    onSetCurrent: () -> Unit,
    onDelete: () -> Unit,
    onUpdateProfile: (ProfileModel) -> Unit,
    onCopyProfile: () -> Unit,
    trainingOnly: Boolean,
) {

    var showEditProfileDialog by state { false }
    var showDeleteConfirmationDialog by state { false }

    ListItem(
        modifier = Modifier.clickable(onClick = onSetCurrent),
        headlineContent = {

            Row {

                Text(name)

                AnimatedVisibility(isCurrent) {

                    Icon(
                        modifier = Modifier.padding(start = 16.dp),
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Current Profile",
                    )
                }
            }
        },
        overlineContent = {
            Text(
                text = if (isTraining) "TRAINING" else "LIVE",
                color = if (isTraining) AppColor.LossRed else AppColor.ProfitGreen,
            )
        },
        supportingContent = { Text(description) },
        trailingContent = {

            Row {

                IconButton(onClick = onCopyProfile) {

                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }

                IconButton(onClick = { showEditProfileDialog = true }) {

                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }

                AnimatedVisibility(!isCurrent) {

                    IconButton(onClick = { showDeleteConfirmationDialog = true }) {

                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        },
    )

    if (showEditProfileDialog) {

        ProfileEditorDialog(
            profileModel = { formValidator ->
                ProfileModel(
                    validator = formValidator,
                    name = name,
                    description = description,
                    isTraining = isTraining,
                )
            },
            onSaveProfile = onUpdateProfile,
            onCloseRequest = { showEditProfileDialog = false },
            trainingOnly = trainingOnly,
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
        modifier = Modifier.width(300.dp),
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        text = {
            Text("Are you sure you want to delete the profile?")
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = contentColorFor(SnackbarDefaults.backgroundColor),
    )
}
