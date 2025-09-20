package com.saurabhsandav.core.ui.stockchart.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.controls.ListSelectionDialog
import com.saurabhsandav.core.ui.common.derivedState
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.trading.core.Timeframe

@Composable
internal fun TimeframeSelectionDialog(
    onDismissRequest: () -> Unit,
    initialFilterQuery: String,
    onSelect: (Timeframe) -> Unit,
    onOpenInCurrentWindow: ((Timeframe) -> Unit)?,
    onOpenInNewWindow: (Timeframe) -> Unit,
) {

    var selectedIndex by state { -1 }
    val filterQuery = rememberTextFieldState(initialFilterQuery)
    val items by derivedState {
        Timeframe.entries.filter { it.toLabel().contains(filterQuery.text, ignoreCase = true) }
    }

    ListSelectionDialog(
        onDismissRequest = onDismissRequest,
        itemCount = { items.size },
        selectedIndex = selectedIndex,
        onSelectionChange = { index -> selectedIndex = index },
        onSelectionFinished = { index -> onSelect(items[index]) },
        title = { Text("Select Timeframe") },
        onKeyEvent = onKeyEvent@{ keyEvent, index ->

            val defaultCondition = keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown
            if (!defaultCondition) return@onKeyEvent false

            when (keyEvent.key) {
                Key.C if onOpenInCurrentWindow != null -> onOpenInCurrentWindow(items[index])
                Key.N -> onOpenInNewWindow(items[index])
                else -> return@onKeyEvent false
            }

            onDismissRequest()

            true
        },
        filterQuery = filterQuery,
        dialogSize = DpSize(width = 250.dp, height = Dp.Unspecified),
    ) { index ->

        val timeframe = items[index]

        ListSelectionItem(
            isSelected = selectedIndex == index,
            onSelect = {
                onSelect(timeframe)
                onDismissRequest()
            },
            headlineContent = { Text(timeframe.toLabel()) },
            trailingContent = {

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
        )
    }
}
