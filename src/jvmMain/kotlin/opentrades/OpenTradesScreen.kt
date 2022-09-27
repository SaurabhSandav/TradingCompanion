package opentrades

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import table.LazyTable
import table.addColumnText
import table.rememberTableSchema
import table.rows
import utils.NIFTY50
import utils.state

@Composable
internal fun OpenTradesScreen(
    presenter: OpenTradesPresenter,
) {

    val state by presenter.state.collectAsState()

    var showTradeCreationDialog by state { false }

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
    }


    /*        item {

                Row(
                    modifier = Modifier.padding(16.dp).fillParentMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Button(onClick = { showTradeCreationDialog = true }) {
                        Text("New Trade")
                    }
                }
            }*/

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
            var quantityIsError by state { false }
            var isLong by state { false }
            var entry by state { "" }
            var entryIsError by state { false }
            var stop by state { "" }
            var stopIsError by state { false }
            var target by state { "" }
            var targetIsError by state { false }

            ListItem(Modifier.clickable { showStockSelectionDialog = true }) {
                Text(ticker)
            }

            TextField(
                value = quantity,
                onValueChange = {
                    quantity = it.trim()
                    quantityIsError = quantity.toBigDecimalOrNull() == null
                },
                label = { Text("Quantity") },
                isError = quantityIsError
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
                onValueChange = {
                    entry = it.trim()
                    entryIsError = entry.toBigDecimalOrNull() == null
                },
                label = { Text("Entry") },
                isError = entryIsError,
            )

            TextField(
                value = stop,
                onValueChange = {
                    stop = it.trim()
                    stopIsError = stop.toBigDecimalOrNull() == null
                },
                label = { Text("Stop") },
                isError = stopIsError,
            )


            TextField(
                value = target,
                onValueChange = {
                    target = it.trim()
                    targetIsError = target.toBigDecimalOrNull() == null
                },
                label = { Text("Target") },
                isError = targetIsError,
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

        var filterQuery by state { "" }
        val focusRequester = remember { FocusRequester() }
        val items = remember(filterQuery) {
            NIFTY50.filter { it.startsWith(filterQuery) }
        }

        BasicTextField(
            value = filterQuery,
            onValueChange = { value ->
                if (value.all { it.isLetter() })
                    filterQuery = value.trim().uppercase()
            },
            modifier = Modifier.size(0.dp, 0.dp).focusRequester(focusRequester)
        )

        SideEffect { focusRequester.requestFocus() }

        LazyColumn {

            items(
                items = items,
                key = { it },
            ) { stock ->

                ListItem(
                    modifier = Modifier.clickable { onTickerSelected(stock) },
                ) {

                    val stockText = remember(filterQuery, stock) {

                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(filterQuery)
                            }

                            append(stock.removePrefix(filterQuery))
                        }
                    }

                    Text(
                        text = stockText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
