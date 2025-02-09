package com.saurabhsandav.core.ui.tickerselectiondialog

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
        itemTrailingContent = (type as? Chart)?.let { type ->
            { ticker ->

                Row {

                    IconButton(
                        onClick = {
                            type.onOpenInNewWindow(ticker)
                            onDismissRequest()
                        }
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = "Open in new window"
                        )
                    }

                    IconButton(
                        onClick = {
                            type.onOpenInNewTab(ticker)
                            onDismissRequest()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.OpenInNew,
                            contentDescription = "Open in new tab"
                        )
                    }
                }
            }
        },
        initialFilterQuery = initialFilterQuery,
    )
}
