package com.saurabhsandav.core.ui.sizing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.common.controls.ListSelectionDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.sizing.model.SizingState.SizedTrade
import com.saurabhsandav.core.ui.sizing.model.SizingState.TradeExecutionFormParams
import com.saurabhsandav.core.ui.tradeexecutionform.TradeExecutionFormWindow
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SizingScreen(
    sizedTrades: ImmutableList<SizedTrade>,
    onUpdateEntry: (id: Long, entry: String) -> Unit,
    onUpdateStop: (id: Long, stop: String) -> Unit,
    onOpenLiveTrade: (Long) -> Unit,
    onDeleteTrade: (Long) -> Unit,
    onAddTrade: (ticker: String) -> Unit,
) {

    // Set window title
    WindowTitle("Trade Sizing")

    SizingTradesGrid(
        sizedTrades = sizedTrades,
        onUpdateEntry = onUpdateEntry,
        onUpdateStop = onUpdateStop,
        onOpenLiveTrade = onOpenLiveTrade,
        onDeleteTrade = onDeleteTrade,
        onAddTrade = onAddTrade,
    )
}

@Composable
internal fun SizingScreenWindows(
    executionFormWindowsManager: AppWindowsManager<TradeExecutionFormParams>,
) {

    // Trade execution form windows
    executionFormWindowsManager.Windows { window ->

        TradeExecutionFormWindow(
            profileId = window.params.profileId,
            formType = window.params.formType,
            onExecutionSaved = window.params.onExecutionSaved,
            onCloseRequest = window::close,
        )
    }
}

@Composable
private fun SizingTradesGrid(
    sizedTrades: ImmutableList<SizedTrade>,
    onUpdateEntry: (id: Long, entry: String) -> Unit,
    onUpdateStop: (id: Long, stop: String) -> Unit,
    onOpenLiveTrade: (Long) -> Unit,
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
                onOpenLiveTrade = { onOpenLiveTrade(sizedTrade.id) },
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
    onOpenLiveTrade: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {

            TextButton(
                onClick = onOpenLiveTrade,
            ) {
                Text("Open Live Trade")
            }

            TextButton(
                onClick = onDeleteTrade,
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
