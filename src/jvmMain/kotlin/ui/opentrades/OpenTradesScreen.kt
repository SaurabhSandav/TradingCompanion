package ui.opentrades

import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ui.common.controls.DateField
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

        row {

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

    Dialog(
        onCloseRequest = onCloseRequest,
        title = "New Trade",
    ) {

        Column(
            modifier = Modifier.width(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            val formState = remember { OpenTradeFormState() }

            var showStockSelectionDialog by state { false }

            ListItem(
                modifier = Modifier.clickable { showStockSelectionDialog = true }
                    .border(1.dp, if (formState.ticker.isError) Color.Red else Color.LightGray),
            ) {
                Text(formState.ticker.value)
            }

            if (showStockSelectionDialog) {

                StockSelectionDialog(
                    onTickerSelected = {
                        formState.ticker.onSelectionChange(it)
                        showStockSelectionDialog = false
                    },
                    onCloseRequest = { showStockSelectionDialog = false },
                )
            }

            TextField(
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

            TextField(
                value = formState.entry.value,
                onValueChange = formState.entry.onValueChange,
                label = { Text("Entry") },
                isError = formState.entry.isError,
                singleLine = true,
            )

            TextField(
                value = formState.stop.value,
                onValueChange = formState.stop.onValueChange,
                label = { Text("Stop") },
                isError = formState.stop.isError,
                singleLine = true,
            )

            var currentDate by state {
                Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            }

            DateField(
                value = currentDate,
                onValidDateChange = { currentDate = it },
                label = { Text("Date") },
            )

            var currentTime by state {
                Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .time
            }

            TimeField(
                value = currentTime,
                onValidTimeChange = { currentTime = it },
                label = { Text("Entry Time") },
            )

            TextField(
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
                            formState.ticker.value,
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
