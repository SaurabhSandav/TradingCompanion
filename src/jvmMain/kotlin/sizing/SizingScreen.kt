package sizing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SizingScreen(
    presenter: SizingPresenter,
) {

    val state by presenter.state.collectAsState()

    LazyColumn {

        stickyHeader {
            SizingListHeader()
        }

        items(
            items = state.sizedTrades
        ) { sizedTrade ->

            SizingListItem(
                sizedTrade = sizedTrade,
                onEntryChanged = { entry -> presenter.updateEntry(sizedTrade, entry) },
                onStopChanged = { stop -> presenter.updateStop(sizedTrade, stop) },
            ) { presenter.removeTrade(it) }
        }

        item {

            SizingTradeCreator(
                onAddTrade = { presenter.addTrade(it) },
            )
        }
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

        Spacer(Modifier.weight(1F))
    }

    Divider()
}

@Composable
private fun SizingListItem(
    sizedTrade: SizedTrade,
    onEntryChanged: (entry: String) -> Unit,
    onStopChanged: (stop: String) -> Unit,
    onRemoveTrade: (ticker: String) -> Unit,
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

        var entry by remember { mutableStateOf(sizedTrade.entry) }

        OutlinedTextField(
            value = entry,
            onValueChange = {
                onEntryChanged(it)
                entry = it
            },
            modifier = Modifier.weight(1F),
            isError = entry.toBigDecimalOrNull() == null,
        )

        var stop by remember { mutableStateOf(sizedTrade.stop) }

        OutlinedTextField(
            value = stop,
            onValueChange = {
                onStopChanged(it)
                stop = it
            },
            modifier = Modifier.weight(1F),
            isError = stop.toBigDecimalOrNull() == null,
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

        IconButton(
            onClick = { onRemoveTrade(sizedTrade.ticker) },
            modifier = Modifier.weight(1F),
        ) {

            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove Trade",
            )
        }
    }
}

@Composable
private fun SizingTradeCreator(
    onAddTrade: (ticker: String) -> Unit,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {

        var ticker by remember { mutableStateOf("") }

        OutlinedTextField(
            value = ticker,
            onValueChange = { ticker = it },
        )

        Button(
            onClick = { onAddTrade(ticker) },
            modifier = Modifier.alignByBaseline(),
        ) {

            Text("New Trade")
        }
    }
}
