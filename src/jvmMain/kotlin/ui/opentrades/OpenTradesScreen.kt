package ui.opentrades

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import ui.common.controls.DateField
import ui.common.controls.ListSelectionField
import ui.common.controls.TimeField
import ui.common.state
import ui.common.table.LazyTable
import ui.common.table.addColumnText
import ui.common.table.rememberTableSchema
import ui.common.table.rows
import utils.NIFTY50

@Composable
internal fun OpenTradesScreen(
    presenter: OpenTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    var showTradeCreationWindow by state { false }

    val schema = rememberTableSchema<OpenTradeListEntry> {
        addColumnText("Broker") { it.broker }
        addColumnText("Ticker") { it.ticker }
        addColumnText("Instrument") { it.instrument }
        addColumnText("Quantity") { it.quantity }
        addColumnText("Side") { it.side }
        addColumnText("Entry") { it.entry }
        addColumnText("Stop") { it.stop }
        addColumnText("Entry Time") { it.entryTime }
        addColumnText("Target") { it.target }
    }

    LazyTable(
        schema = schema,
    ) {

        rows(
            items = state.openTrades,
            key = { it.id },
        )

        row {

            Row(
                modifier = Modifier.padding(16.dp).fillParentMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Button(onClick = { showTradeCreationWindow = true }) {
                    Text("New Trade")
                }
            }
        }
    }

    if (showTradeCreationWindow) {

        OpenTradeCreationWindow(
            onCloseRequest = { showTradeCreationWindow = false },
            onAddTrade = { ticker, quantity, isLong, entry, stop, target ->
                presenter.addTrade(
                    ticker = ticker,
                    quantity = quantity,
                    isLong = isLong,
                    entry = entry,
                    stop = stop,
                    target = target,
                )
                showTradeCreationWindow = false
            },
        )
    }
}

@Composable
private fun OpenTradeCreationWindow(
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

    val windowState = rememberWindowState(
        size = DpSize(width = 300.dp, height = Dp.Unspecified),
    )

    Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = "New Trade",
        resizable = false,
    ) {

        Column(
            modifier = Modifier.padding(16.dp).width(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            val formState = remember { OpenTradeFormState() }

            DateField(
                value = formState.date.value,
                onValidValueChange = formState.date.onValueChange,
                label = { Text("Date") },
            )

            ListSelectionField(
                items = NIFTY50,
                onSelection = formState.ticker.onSelectionChange,
                selection = formState.ticker.value,
                isError = formState.ticker.isError,
            )

            OutlinedTextField(
                value = formState.quantity.value,
                onValueChange = formState.quantity.onValueChange,
                label = { Text("Quantity") },
                isError = formState.quantity.isError,
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("Long")

                Switch(
                    checked = formState.isLong.value,
                    onCheckedChange = formState.isLong.onCheckedChange,
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colors.secondaryVariant,
                        uncheckedTrackColor = MaterialTheme.colors.secondaryVariant,
                        uncheckedTrackAlpha = 0.54f,
                    )
                )

                Text("Short")
            }

            OutlinedTextField(
                value = formState.entry.value,
                onValueChange = formState.entry.onValueChange,
                label = { Text("Entry") },
                isError = formState.entry.isError,
                singleLine = true,
            )

            OutlinedTextField(
                value = formState.stop.value,
                onValueChange = formState.stop.onValueChange,
                label = { Text("Stop") },
                isError = formState.stop.isError,
                singleLine = true,
            )

            TimeField(
                value = formState.entryTime.value,
                onValidValueChange = formState.entryTime.onValueChange,
                label = { Text("Entry Time") },
            )

            OutlinedTextField(
                value = formState.target.value,
                onValueChange = formState.target.onValueChange,
                label = { Text("Target") },
                isError = formState.target.isError,
                singleLine = true,
            )

            Button(
                onClick = {
                    if (formState.isValid()) {
                        onAddTrade(
                            formState.ticker.value!!,
                            formState.quantity.value,
                            formState.isLong.value,
                            formState.entry.value,
                            formState.stop.value,
                            formState.target.value
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {

                Text("Add")
            }
        }
    }
}
