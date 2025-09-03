package com.saurabhsandav.core.ui.trade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.DeleteConfirmationDialog
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.trade.StopPreviewer
import com.saurabhsandav.core.ui.trade.TargetPreviewer
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeStop
import com.saurabhsandav.core.ui.trade.model.TradeState.TradeTarget
import com.saurabhsandav.kbigdecimal.KBigDecimal
import kotlinx.coroutines.flow.Flow

@Composable
internal fun StopsAndTargets(
    stops: List<TradeStop>,
    stopPreviewer: Flow<StopPreviewer>,
    onAddStop: (KBigDecimal) -> Unit,
    onDeleteStop: (KBigDecimal) -> Unit,
    onSetPrimaryStop: (KBigDecimal) -> Unit,
    targets: List<TradeTarget>,
    showTargetRValues: Boolean,
    targetPreviewer: Flow<TargetPreviewer>,
    onAddTarget: (KBigDecimal) -> Unit,
    onDeleteTarget: (KBigDecimal) -> Unit,
    onSetPrimaryTarget: (KBigDecimal) -> Unit,
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.rowHorizontalSpacing),
    ) {

        StopsList(
            modifier = Modifier.weight(1F),
            stops = stops,
            stopPreviewer = stopPreviewer,
            onAddStop = onAddStop,
            onDeleteStop = onDeleteStop,
            onSetPrimaryStop = onSetPrimaryStop,
        )

        TargetsList(
            modifier = Modifier.weight(1F),
            targets = targets,
            showRValues = showTargetRValues,
            targetPreviewer = targetPreviewer,
            onAddTarget = onAddTarget,
            onDeleteTarget = onDeleteTarget,
            onSetPrimaryTarget = onSetPrimaryTarget,
        )
    }
}

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
