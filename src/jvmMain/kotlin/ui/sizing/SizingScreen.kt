package ui.sizing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ui.common.LocalAppWindowState
import ui.common.controls.ListSelectionDialog
import ui.common.state
import ui.opentradeform.OpenTradeFormWindow
import ui.sizing.model.SizedTrade
import ui.sizing.model.SizingEvent
import utils.NIFTY50

@Composable
internal fun SizingScreen(
    presenter: SizingPresenter,
) {

    val state by presenter.state.collectAsState()
    val appWindowState = LocalAppWindowState.current

    // Set window title
    LaunchedEffect(appWindowState) { appWindowState.title = "Trade Sizing" }

    SizingTradesGrid(
        sizedTrades = state.sizedTrades,
        onUpdateEntry = { id, entry -> presenter.event(SizingEvent.UpdateTradeEntry(id, entry)) },
        onUpdateStop = { id, stop -> presenter.event(SizingEvent.UpdateTradeStop(id, stop)) },
        onOpenTrade = { presenter.event(SizingEvent.OpenTrade(it)) },
        onDeleteTrade = { presenter.event(SizingEvent.RemoveTrade(it)) },
        onAddTrade = { presenter.event(SizingEvent.AddTrade(it)) },
    )

    // Add open trade windows
    state.openTradeFormWindowStates.forEach { windowState ->

        key(windowState) {

            OpenTradeFormWindow(windowState)
        }
    }
}

@Composable
private fun SizingTradesGrid(
    sizedTrades: List<SizedTrade>,
    onUpdateEntry: (id: Long, entry: String) -> Unit,
    onUpdateStop: (id: Long, stop: String) -> Unit,
    onOpenTrade: (Long) -> Unit,
    onDeleteTrade: (Long) -> Unit,
    onAddTrade: (ticker: String) -> Unit,
) {

    LazyVerticalGrid(
        modifier = Modifier.padding(8.dp),
        columns = GridCells.Adaptive(250.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        items(
            items = sizedTrades,
            key = { it.id },
        ) { sizedTrade ->

            SizingTradeCard(
                sizedTrade = sizedTrade,
                onUpdateEntry = { entry -> onUpdateEntry(sizedTrade.id, entry) },
                onUpdateStop = { stop -> onUpdateStop(sizedTrade.id, stop) },
                onOpenTrade = { onOpenTrade(sizedTrade.id) },
                onDeleteTrade = { onDeleteTrade(sizedTrade.id) },
            )
        }

        item {
            AddTradeCard(onAddTrade)
        }
    }
}

@Composable
private fun SizingTradeCard(
    sizedTrade: SizedTrade,
    onUpdateEntry: (String) -> Unit,
    onUpdateStop: (String) -> Unit,
    onOpenTrade: () -> Unit,
    onDeleteTrade: () -> Unit,
) {

    Card {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(sizedTrade.ticker)

            var entry by state { sizedTrade.entry }

            OutlinedTextField(
                value = entry,
                onValueChange = {
                    onUpdateEntry(it)
                    entry = it
                },
                isError = entry.toBigDecimalOrNull() == null,
                singleLine = true,
                label = { Text("Entry") },
            )

            var stop by state { sizedTrade.stop }

            OutlinedTextField(
                value = stop,
                onValueChange = {
                    onUpdateStop(it)
                    stop = it
                },
                isError = stop.toBigDecimalOrNull() == null,
                singleLine = true,
                label = { Text("Stop") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {

                Text("Side")

                Text(
                    text = sizedTrade.side,
                    color = sizedTrade.color,
                    fontWeight = FontWeight.Companion.Bold
                )
            }

            val data = remember(sizedTrade) {
                listOf(
                    "Spread" to sizedTrade.spread,
                    "Quantity (Calc)" to sizedTrade.calculatedQuantity,
                    "Quantity (Max)" to sizedTrade.maxAffordableQuantity,
                    "Target (1R)" to sizedTrade.target,
                )
            }

            data.forEach { (label, value) ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {

                    Text(label)

                    Text(value)
                }
            }
        }

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {

            TextButton(
                onClick = { onOpenTrade() },
                modifier = Modifier.weight(1F),
            ) {
                Text("Open Trade")
            }

            TextButton(
                onClick = { onDeleteTrade() },
                modifier = Modifier.weight(1F),
            ) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun AddTradeCard(
    onAddTrade: (ticker: String) -> Unit,
) {

    Card {

        var showStockSelectionDialog by state { false }

        TextButton(
            onClick = { showStockSelectionDialog = true },
        ) {

            Text(
                text = "New Trade",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        if (showStockSelectionDialog) {

            ListSelectionDialog(
                items = NIFTY50,
                onSelection = {
                    onAddTrade(it)
                    showStockSelectionDialog = false
                },
                selectionDialogTitle = "Select Stock",
                onCloseRequest = { showStockSelectionDialog = false },
            )
        }
    }
}
