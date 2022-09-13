package sizing

import AppModule
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PositionSizer(
    appModule: AppModule,
) {
    val presenter = remember { SizingPresenter(appModule) }
    val state by presenter.state.collectAsState()

    Column {

        val positionSizerTableState = remember { PositionSizerTableState() }

        PositionSizerHeader(positionSizerTableState)

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {

            for (sizedTrade in state.sizedTrades) {

                PositionSizerEntry(
                    positionSizerTableState = positionSizerTableState,
                    sizedTrade = sizedTrade,
                    onEntryChanged = { entry -> presenter.updateEntry(sizedTrade, entry) },
                    onStopChanged = { stop -> presenter.updateStop(sizedTrade, stop) },
                ) { presenter.removeTrade(it) }
            }

            AddTrade(
                onAddTrade = { presenter.addTrade(it) },
            )
        }
    }
}

@Composable
private fun PositionSizerHeader(
    positionSizerTableState: PositionSizerTableState,
) {

    Row(Modifier.padding(16.dp)) {

        Text(
            text = "Ticker",
            modifier = Modifier.weight(positionSizerTableState.tickerWeight),
        )

        Text(
            text = "Entry",
            modifier = Modifier.weight(positionSizerTableState.entryWeight),
        )

        Text(
            text = "Stop",
            modifier = Modifier.weight(positionSizerTableState.stopWeight),
        )

        Text(
            text = "Calculated Quantity",
            modifier = Modifier.weight(positionSizerTableState.calculatedQuantityWeight),
        )

        Text(
            text = "Max Affordable Quantity",
            modifier = Modifier.weight(positionSizerTableState.maxAffordableQuantityWeight),
        )

        Text(
            text = "Entry Quantity",
            modifier = Modifier.weight(positionSizerTableState.entryQuantityWeight),
        )

        Text(
            text = "Target (1x)",
            modifier = Modifier.weight(positionSizerTableState.target1xWeight),
        )

        Spacer(Modifier.weight(positionSizerTableState.deleteWeight))
    }
}

@Composable
private fun PositionSizerEntry(
    positionSizerTableState: PositionSizerTableState,
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
            modifier = Modifier.weight(positionSizerTableState.tickerWeight),
        )

        var entry by remember(sizedTrade.entry) { mutableStateOf(sizedTrade.entry) }

        OutlinedTextField(
            value = entry,
            onValueChange = {
                onEntryChanged(it)
                entry = it
            },
            modifier = Modifier.weight(positionSizerTableState.entryWeight),
            isError = entry.toBigDecimalOrNull() == null,
        )

        var stop by remember(sizedTrade.stop) { mutableStateOf(sizedTrade.stop) }

        OutlinedTextField(
            value = stop,
            onValueChange = {
                onStopChanged(it)
                stop = it
            },
            modifier = Modifier.weight(positionSizerTableState.stopWeight),
            isError = stop.toBigDecimalOrNull() == null,
        )

        Text(
            text = sizedTrade.calculatedQuantity,
            modifier = Modifier.weight(positionSizerTableState.calculatedQuantityWeight),
        )

        Text(
            text = sizedTrade.maxAffordableQuantity,
            modifier = Modifier.weight(positionSizerTableState.maxAffordableQuantityWeight),
        )

        Text(
            text = sizedTrade.entryQuantity,
            modifier = Modifier.weight(positionSizerTableState.entryWeight),
        )

        Text(
            text = sizedTrade.target,
            modifier = Modifier.weight(positionSizerTableState.target1xWeight),
        )

        IconButton(
            onClick = { onRemoveTrade(sizedTrade.ticker) },
            modifier = Modifier.weight(positionSizerTableState.deleteWeight),
        ) {

            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove Trade",
            )
        }
    }
}

@Composable
private fun AddTrade(
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
