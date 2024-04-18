package com.saurabhsandav.core.ui.profiles.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.profiles.form.ProfileFormDialog
import com.saurabhsandav.core.ui.profiles.form.ProfileFormType
import com.saurabhsandav.core.ui.profiles.model.ProfilesState.Profile
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun ProfileListItem(
    profile: Profile,
    onSelectProfile: () -> Unit,
    isCurrent: Boolean,
    onSetCurrentProfile: () -> Unit,
    onDeleteProfile: () -> Unit,
    onCopyProfile: () -> Unit,
    trainingOnly: Boolean,
) {

    var showEditProfileDialog by state { false }
    var showDeleteConfirmationDialog by state { false }

    ListItem(
        modifier = Modifier.clickable(onClick = onSelectProfile).padding(MaterialTheme.dimens.listItemPadding),
        headlineContent = {

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing)
            ) {

                Text(profile.name)

                AnimatedVisibility(isCurrent) {

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Current Profile",
                    )
                }
            }
        },
        overlineContent = {

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
            ) {

                Text(
                    text = if (profile.isTraining) "TRAINING" else "LIVE",
                    color = if (profile.isTraining) AppColor.LossRed else AppColor.ProfitGreen,
                )

                Text(
                    text = buildString {
                        append(profile.tradeCount)
                        append(" trades")

                        if (profile.tradeCountOpen != null) {
                            append(" (")
                            append(profile.tradeCountOpen)
                            append(" open)")
                        }
                    }
                )
            }
        },
        supportingContent = profile.description?.let { { Text(it) } },
        trailingContent = {

            Row {

                if (!isCurrent) {

                    IconButtonWithTooltip(
                        onClick = onSetCurrentProfile,
                        tooltipText = "Set Current",
                        content = {
                            Icon(Icons.Default.Check, contentDescription = "Set Current")
                        },
                    )
                }

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

    HorizontalDivider()

    if (showEditProfileDialog) {

        ProfileFormDialog(
            type = remember { ProfileFormType.Edit(profile.id) },
            trainingOnly = trainingOnly,
            onCloseRequest = { showEditProfileDialog = false },
        )
    }

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = "profile",
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDeleteProfile,
        )
    }
}
