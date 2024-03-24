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
import com.saurabhsandav.core.trades.model.SizingTradeId
import com.saurabhsandav.core.ui.common.app.AppWindowsManager
import com.saurabhsandav.core.ui.common.app.WindowTitle
import com.saurabhsandav.core.ui.common.controls.ListSelectionDialog
import com.saurabhsandav.core.ui.common.state
import com.saurabhsandav.core.ui.sizing.model.SizingState.SizedTrade
import com.saurabhsandav.core.ui.sizing.model.SizingState.TradeExecutionFormParams
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tradeexecutionform.TradeExecutionFormWindow
import com.saurabhsandav.core.utils.NIFTY50

@Composable
internal fun SizingScreen(
    sizedTrades: List<SizedTrade>,
    onUpdateEntry: (id: SizingTradeId, entry: String) -> Unit,
    onUpdateStop: (id: SizingTradeId, stop: String) -> Unit,
    onOpenLiveTrade: (SizingTradeId) -> Unit,
    onDeleteTrade: (SizingTradeId) -> Unit,
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
            onCloseRequest = window::close,
        )
    }
}

@Composable
private fun SizingTradesGrid(
    sizedTrades: List<SizedTrade>,
    onUpdateEntry: (id: SizingTradeId, entry: String) -> Unit,
    onUpdateStop: (id: SizingTradeId, stop: String) -> Unit,
    onOpenLiveTrade: (SizingTradeId) -> Unit,
    onDeleteTrade: (SizingTradeId) -> Unit,
    onAddTrade: (ticker: String) -> Unit,
) {

    LazyVerticalGrid(
        modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
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
            modifier = Modifier.padding(MaterialTheme.dimens.containerPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.columnVerticalSpacing),
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

        HorizontalDivider()

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

        var showTickerSelectionDialog by state { false }

        TextButton(
            onClick = { showTickerSelectionDialog = true },
        ) {

            Text(
                text = "New Trade",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        if (showTickerSelectionDialog) {

            ListSelectionDialog(
                onDismissRequest = { showTickerSelectionDialog = false },
                items = NIFTY50,
                itemText = { it },
                onSelection = {
                    onAddTrade(it)
                    showTickerSelectionDialog = false
                },
                placeholderText = "Select Ticker...",
            )
        }
    }
}
