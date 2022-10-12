package ui.sizing

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ListItem
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ui.common.state
import ui.common.table.*
import utils.NIFTY50

@Composable
internal fun SizingScreen(
    presenter: SizingPresenter,
) {

    val state by presenter.state.collectAsState()

    var showStockSelectionDialog by state { false }

    val schema = rememberTableSchema<SizedTrade> {
        addColumnText("Ticker") { it.ticker }
        addColumn("Entry") { sizedTrade ->

            var entry by state { sizedTrade.entry }

            OutlinedTextField(
                value = entry,
                onValueChange = {
                    presenter.updateEntry(sizedTrade, entry)
                    entry = it
                },
                isError = entry.toBigDecimalOrNull() == null,
                singleLine = true,
            )
        }
        addColumn("Stop") { sizedTrade ->

            var stop by state { sizedTrade.stop }

            OutlinedTextField(
                value = stop,
                onValueChange = {
                    presenter.updateStop(sizedTrade, stop)
                    stop = it
                },
                isError = stop.toBigDecimalOrNull() == null,
                singleLine = true,
            )
        }
        addColumnText("Side") { it.side }
        addColumnText("Spread") { it.spread }
        addColumnText("Calculated Quantity") { it.calculatedQuantity }
        addColumnText("Max Affordable Quantity") { it.maxAffordableQuantity }
        addColumnText("Entry Quantity") { it.entryQuantity }
        addColumnText("Target (1x)") { it.target }
    }

    LazyTable(
        schema = schema,
    ) {

        rows(
            items = state.sizedTrades,
            key = { it.ticker },
        ) { sizedTrade ->

            ContextMenuArea(
                items = {
                    listOf(
                        ContextMenuItem("Delete") { presenter.removeTrade(sizedTrade.ticker) },
                    )
                },
            ) {

                DefaultTableRow(
                    item = sizedTrade,
                    schema = schema,
                )
            }
        }

        row {

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
