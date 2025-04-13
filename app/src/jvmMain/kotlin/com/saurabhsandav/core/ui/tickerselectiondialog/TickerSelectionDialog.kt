package com.saurabhsandav.core.ui.tickerselectiondialog

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.saurabhsandav.core.ui.common.controls.LazyListSelectionDialog
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType.Chart
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType.Regular
import com.saurabhsandav.core.ui.tickerselectiondialog.model.TickerSelectionEvent.Filter

@Composable
fun TickerSelectionDialog(
    onDismissRequest: () -> Unit,
    tickers: List<String>,
    onSelect: (String) -> Unit,
    type: TickerSelectionType = Regular,
    initialFilterQuery: String = "",
) {

    val scope = rememberCoroutineScope()
    val presenter = remember { TickerSelectionPresenter(scope, tickers) }
    val state by presenter.state.collectAsState()

    LazyListSelectionDialog(
        onDismissRequest = onDismissRequest,
        items = state.tickers,
        itemText = { it },
        onSelect = onSelect,
        onFilter = { query -> state.eventSink(Filter(query)) },
        title = { Text("Select Ticker") },
        onKeyEvent = onKeyEvent@{ keyEvent, ticker ->

            if (type !is Chart) return@onKeyEvent false

            val defaultCondition = keyEvent.isCtrlPressed && keyEvent.type == KeyEventType.KeyDown
            if (!defaultCondition) return@onKeyEvent false

            when (keyEvent.key) {
                Key.C if type.onOpenInCurrentWindow != null -> type.onOpenInCurrentWindow(ticker)
                Key.N -> type.onOpenInNewWindow(ticker)
                else -> return@onKeyEvent false
            }

            true
        },
        itemTrailingContent = (type as? Chart)?.let { type ->
            { ticker ->

                Row {

                    IconButton(
                        onClick = {
                            type.onOpenInNewWindow(ticker)
                            onDismissRequest()
                        },
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = "Open in new window",
                        )
                    }

                    val onOpenInCurrentWindow = type.onOpenInCurrentWindow
                    if (onOpenInCurrentWindow != null) {

                        IconButton(
                            onClick = {
                                onOpenInCurrentWindow(ticker)
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
            }
        },
        initialFilterQuery = initialFilterQuery,
    )
}
