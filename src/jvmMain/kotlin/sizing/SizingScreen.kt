package sizing

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import utils.NIFTY50
import utils.state

@Composable
internal fun SizingScreen(
    presenter: SizingPresenter,
) {

    val state by presenter.state.collectAsState()

    var showStockSelectionDialog by state { false }

    LazyColumn {

        stickyHeader {
            SizingListHeader()
        }

        items(
            items = state.sizedTrades,
            key = { it.ticker },
        ) { sizedTrade ->

            ContextMenuArea(items = {
                listOf(
                    ContextMenuItem("Delete") { presenter.removeTrade(sizedTrade.ticker) },
                )
            }) {
                SizingListItem(
                    sizedTrade = sizedTrade,
                    onEntryChanged = { entry -> presenter.updateEntry(sizedTrade, entry) },
                    onStopChanged = { stop -> presenter.updateStop(sizedTrade, stop) },
                )
            }
        }

        item {

            Row(
                modifier = Modifier.padding(16.dp).fillParentMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Button(
                    onClick = { showStockSelectionDialog = true },
                ) {

                    Text("New Trade")
                }
            }
        }
    }

    if (showStockSelectionDialog) {

        StockSelectionDialog(
            onTickerSelected = {
                presenter.addTrade(it)
                showStockSelectionDialog = false
            },
            onCloseRequest = { showStockSelectionDialog = false },
        )
    }
}

@Composable
private fun SizingListHeader() {

    Row(Modifier.padding(16.dp)) {

        Text(
            text = "Ticker",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Entry",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Stop",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Side",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Spread",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Calculated Quantity",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Max Affordable Quantity",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Entry Quantity",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Target (1x)",
            modifier = Modifier.weight(1F),
        )
    }

    Divider()
}

@Composable
private fun SizingListItem(
    sizedTrade: SizedTrade,
    onEntryChanged: (entry: String) -> Unit,
    onStopChanged: (stop: String) -> Unit,
) {

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = sizedTrade.ticker,
            modifier = Modifier.weight(1F),
        )

        var entry by state { sizedTrade.entry }

        OutlinedTextField(
            value = entry,
            onValueChange = {
                onEntryChanged(it)
                entry = it
            },
            modifier = Modifier.weight(1F),
            isError = entry.toBigDecimalOrNull() == null,
            singleLine = true,
        )

        var stop by state { sizedTrade.stop }

        OutlinedTextField(
            value = stop,
            onValueChange = {
                onStopChanged(it)
                stop = it
            },
            modifier = Modifier.weight(1F),
            isError = stop.toBigDecimalOrNull() == null,
            singleLine = true,
        )

        Text(
            text = sizedTrade.side,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = sizedTrade.spread,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = sizedTrade.calculatedQuantity,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = sizedTrade.maxAffordableQuantity,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = sizedTrade.entryQuantity,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = sizedTrade.target,
            modifier = Modifier.weight(1F),
        )
    }
}

@Composable
private fun StockSelectionDialog(
    onTickerSelected: (ticker: String) -> Unit,
    onCloseRequest: () -> Unit,
) {

    Dialog(
        onCloseRequest = onCloseRequest,
        title = "Select Stock",
    ) {

        LazyColumn {

            items(
                items = NIFTY50,
                key = { it },
            ) { stock ->

                ListItem(
                    modifier = Modifier.clickable { onTickerSelected(stock) },
                ) {

                    Text(
                        text = stock,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
