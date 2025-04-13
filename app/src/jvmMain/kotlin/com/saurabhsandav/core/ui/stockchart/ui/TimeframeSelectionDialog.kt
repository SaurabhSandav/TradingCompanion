package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.controls.ListSelectionDialog
import com.saurabhsandav.core.ui.common.toLabel

@Composable
internal fun TimeframeSelectionDialog(
    onDismissRequest: () -> Unit,
    timeframes: List<Timeframe>,
    initialFilterQuery: String,
    onSelect: (Timeframe) -> Unit,
    onOpenInCurrentWindow: ((Timeframe) -> Unit)?,
    onOpenInNewWindow: (Timeframe) -> Unit,
) {

    ListSelectionDialog(
        onDismissRequest = onDismissRequest,
        items = timeframes,
        itemText = { it.toLabel() },
        onSelect = onSelect,
        title = { Text("Select Timeframe") },
        onKeyEvent = onKeyEvent@{ keyEvent, timeframe ->

            val defaultCondition = keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown
            if (!defaultCondition) return@onKeyEvent false

            when (keyEvent.key) {
                Key.C if onOpenInCurrentWindow != null -> onOpenInCurrentWindow(timeframe)
                Key.N -> onOpenInNewWindow(timeframe)
                else -> return@onKeyEvent false
            }

            true
        },
        itemTrailingContent = { timeframe ->

            Row {

                IconButton(
                    onClick = {
                        onOpenInNewWindow(timeframe)
                        onDismissRequest()
                    },
                ) {
                    Icon(
                        Icons.Default.OpenInBrowser,
                        contentDescription = "Open in new window",
                    )
                }

                if (onOpenInCurrentWindow != null) {

                    IconButton(
                        onClick = {
                            onOpenInCurrentWindow(timeframe)
                            onDismissRequest()
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.OpenInNew,
                            contentDescription = "Open in current Window",
                        )
                    }
                }
            }
        },
        initialFilterQuery = initialFilterQuery,
        dialogSize = DpSize(width = 250.dp, height = Dp.Unspecified),
    )
}
