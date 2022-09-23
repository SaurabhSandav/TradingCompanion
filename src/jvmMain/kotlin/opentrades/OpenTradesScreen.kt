package opentrades

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
internal fun OpenTradesScreen(
    presenter: OpenTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    var showTradeCreationDialog by state { false }

    LazyColumn {

        stickyHeader {

            OpenTradeListHeader()

            Divider()
        }

        items(state.openTrades) { openTradeListEntry ->

            OpenTradeListItem(
                entry = openTradeListEntry,
            )

            Divider()
        }

        item {

            Row(
                modifier = Modifier.padding(16.dp).fillParentMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Button(onClick = { showTradeCreationDialog = true }) {
                    Text("New Trade")
                }
            }
        }
    }

    if (showTradeCreationDialog) {

        OpenTradeCreationDialog(
            onCloseRequest = { showTradeCreationDialog = false },
            onAddTrade = { ticker, quantity, isLong, entry, stop, target ->
                presenter.addTrade(
                    ticker = ticker,
                    quantity = quantity,
                    isLong = isLong,
                    entry = entry,
                    stop = stop,
                    target = target,
                )
                showTradeCreationDialog = false
            },
        )
    }
}

@Composable
private fun OpenTradeListHeader() {

    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = "Broker",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Ticker",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Instrument",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Quantity",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Side",
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
            text = "Entry Time",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = "Target",
            modifier = Modifier.weight(1F),
        )
    }
}

@Composable
private fun OpenTradeListItem(
    entry: OpenTradeListEntry,
) {

    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = entry.broker,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.ticker,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.instrument,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.quantity,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.side,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.entry,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.stop ?: "NA",
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.entryTime,
            modifier = Modifier.weight(1F),
        )

        Text(
            text = entry.target ?: "NA",
            modifier = Modifier.weight(1F),
        )
    }
}

@Composable
private fun OpenTradeCreationDialog(
    onCloseRequest: () -> Unit,
    onAddTrade: (
        ticker: String,
        quantity: String,
        isLong: Boolean,
        entry: String,
        stop: String,
        target: String,
    ) -> Unit,
) {

    var showStockSelectionDialog by state { false }

    var ticker by state { "Select Stock..." }

    Dialog(
        onCloseRequest = onCloseRequest,
        title = "New Trade",
    ) {

        Column(Modifier.width(IntrinsicSize.Min)) {

            var quantity by state { "" }
            var isLong by state { false }
            var entry by state { "" }
            var stop by state { "" }
            var target by state { "" }

            ListItem(Modifier.clickable { showStockSelectionDialog = true }) {
                Text(ticker)
            }

            TextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("Long")

                Switch(
                    checked = isLong,
                    onCheckedChange = { isLong = !isLong },
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colors.secondaryVariant,
                        uncheckedTrackColor = MaterialTheme.colors.secondaryVariant,
                        uncheckedTrackAlpha = 0.54f,
                    )
                )

                Text("Short")
            }

            TextField(
                value = entry,
                onValueChange = { entry = it },
                label = { Text("Entry") },
            )

            TextField(
                value = stop,
                onValueChange = { stop = it },
                label = { Text("Stop") },
            )


            TextField(
                value = target,
                onValueChange = { target = it },
                label = { Text("Target") },
            )

            Button(
                onClick = { onAddTrade(ticker, quantity, isLong, entry, stop, target) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {

                Text("Add")
            }
        }
    }

    if (showStockSelectionDialog) {

        StockSelectionDialog(
            onTickerSelected = {
                ticker = it
                showStockSelectionDialog = false
            },
            onCloseRequest = { showStockSelectionDialog = false },
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
