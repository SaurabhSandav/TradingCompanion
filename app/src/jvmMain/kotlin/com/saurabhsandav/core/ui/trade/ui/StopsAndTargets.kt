package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state

@Composable
internal fun DeleteIconButton(
    deleteTypeText: String,
    onDelete: () -> Unit,
) {

    var showDeleteConfirmationDialog by state { false }

    IconButtonWithTooltip(
        onClick = { showDeleteConfirmationDialog = true },
        tooltipText = "Delete $deleteTypeText",
        content = {
            Icon(Icons.Default.Close, contentDescription = "Delete $deleteTypeText")
        },
    )

    if (showDeleteConfirmationDialog) {

        DeleteConfirmationDialog(
            subject = deleteTypeText,
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirm = onDelete,
        )
    }
}

@Composable
internal fun ToggleIsPrimaryButton(
    typeText: String,
    isPrimary: Boolean,
    onToggle: () -> Unit,
) {

    IconButtonWithTooltip(
        onClick = onToggle,
        enabled = !isPrimary,
        tooltipText = when {
            isPrimary -> "Primary $typeText"
            else -> "Set Primary"
        },
        content = {

            Icon(
                imageVector = when {
                    isPrimary -> Icons.Filled.CheckCircle
                    else -> Icons.Outlined.CheckCircleOutline
                },
                contentDescription = when {
                    isPrimary -> "Primary $typeText"
                    else -> "Secondary $typeText"
                },
            )
        },
    )
}

internal val StopTargetOptionsWidth = 100.dp
