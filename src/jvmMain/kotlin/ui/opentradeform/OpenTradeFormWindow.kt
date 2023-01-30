package ui.opentradeform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import optionalContent
import ui.common.app.AppWindow
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.form.isError
import utils.NIFTY50

@Composable
internal fun OpenTradeFormWindow(
    state: OpenTradeFormWindowState,
) {

    val windowState = rememberWindowState()

    AppWindow(
        onCloseRequest = state.params.onCloseRequest,
        state = windowState,
        title = "New Trade",
    ) {

        Box(Modifier.wrapContentSize()) {

            when {
                state.isReady -> OpenTradeForm(state)
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun OpenTradeForm(state: OpenTradeFormWindowState) {

    Column(
        modifier = Modifier.padding(16.dp).width(IntrinsicSize.Min).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        val model = state.model

        ListSelectionField(
            items = NIFTY50,
            onSelection = { model.ticker.value = it },
            selection = model.ticker.value,
            label = { Text("Ticker") },
            isError = model.ticker.isError,
            supportingText = optionalContent(model.ticker.errorMessage) { Text(it) },
        )

        OutlinedTextField(
            value = model.quantity.value,
            onValueChange = { model.quantity.value = it.trim() },
            label = { Text("Quantity") },
            isError = model.quantity.isError,
            supportingText = optionalContent(model.quantity.errorMessage) { Text(it) },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Short")

            Switch(
                checked = model.isLong.value,
                onCheckedChange = { model.isLong.value = it },
            )

            Text("Long")
        }

        OutlinedTextField(
            value = model.entry.value,
            onValueChange = { model.entry.value = it.trim() },
            label = { Text("Entry") },
            isError = model.entry.isError,
            supportingText = optionalContent(model.entry.errorMessage) { Text(it) },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.stop.value,
            onValueChange = { model.stop.value = it.trim() },
            label = { Text("Stop") },
            isError = model.stop.isError,
            supportingText = optionalContent(model.stop.errorMessage) { Text(it) },
            singleLine = true,
        )

        DateTimeField(
            value = model.entryDateTime.value,
            onValidValueChange = { model.entryDateTime.value = it },
            label = { Text("Entry DateTime") },
            isError = model.entryDateTime.isError,
            supportingText = optionalContent(model.entryDateTime.errorMessage) { Text(it) },
        )

        OutlinedTextField(
            value = model.target.value,
            onValueChange = { model.target.value = it.trim() },
            label = { Text("Target") },
            isError = model.target.isError,
            supportingText = optionalContent(model.target.errorMessage) { Text(it) },
            singleLine = true,
        )

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = state::onSaveTrade,
        ) {

            Text("Add")
        }
    }
}
