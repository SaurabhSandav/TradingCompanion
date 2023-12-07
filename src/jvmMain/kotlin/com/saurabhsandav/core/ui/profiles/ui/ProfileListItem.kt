package com.saurabhsandav.core.ui.profiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.form.ProfileFormDialog
import com.saurabhsandav.core.ui.profiles.form.ProfileFormType
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile

@Composable
internal fun ProfileListItem(
    profile: Profile,
    isCurrent: Boolean,
    onSetCurrentProfile: () -> Unit,
    onDeleteProfile: () -> Unit,
    onCopyProfile: () -> Unit,
    trainingOnly: Boolean,
) {

    var showEditProfileDialog by state { false }
    var showDeleteConfirmationDialog by state { false }

    ListItem(
        modifier = Modifier.clickable(onClick = onSetCurrentProfile),
        headlineContent = {

            Row {

                Text(profile.name)

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
                text = if (profile.isTraining) "TRAINING" else "LIVE",
                color = if (profile.isTraining) AppColor.LossRed else AppColor.ProfitGreen,
            )
        },
        supportingContent = { Text(profile.description) },
        trailingContent = {

            Row {

                IconButtonWithTooltip(
                    onClick = onCopyProfile,
                    tooltipText = "Copy",
                    content = {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    },
                )

                IconButtonWithTooltip(
                    onClick = { showEditProfileDialog = true },
                    tooltipText = "Edit",
                    content = {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    },
                )

                AnimatedVisibility(!isCurrent) {

                    IconButtonWithTooltip(
                        onClick = { showDeleteConfirmationDialog = true },
                        tooltipText = "Delete",
                        content = {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        },
                    )
                }
            }
        },
    )

    if (showEditProfileDialog) {

        ProfileFormDialog(
            type = remember { ProfileFormType.Edit(profile.id) },
            trainingOnly = trainingOnly,
            onCloseRequest = { showEditProfileDialog = false },
        )
    }

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDeleteProfile,
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
            Text("Are you sure you want to delete the profile?")
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        },
    )
}
