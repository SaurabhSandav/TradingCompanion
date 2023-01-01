package ui.closetradeform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import ui.common.AppWindow
import ui.common.OutlinedTextField
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.flow.FlowRow
import ui.common.form.isError
import utils.NIFTY50

@Composable
internal fun CloseTradeFormWindow(
    state: CloseTradeFormWindowState,
) {

    val windowState = rememberWindowState()

    AppWindow(
        onCloseRequest = state.params.onCloseRequest,
        state = windowState,
        title = "Close Trade",
    ) {

        Box(Modifier.wrapContentSize()) {

            when {
                state.isReady -> MainForm(state)
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun MainForm(state: CloseTradeFormWindowState) {

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
            errorText = { Text(model.ticker.errorMessage) },
        )

        OutlinedTextField(
            value = model.quantity.value,
            onValueChange = { model.quantity.value = it.trim() },
            label = { Text("Quantity") },
            isError = model.quantity.isError,
            errorText = { Text(model.quantity.errorMessage) },
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
            onValueChange = { model.entry.value = (it.trim()) },
            label = { Text("Entry") },
            isError = model.entry.isError,
            errorText = { Text(model.entry.errorMessage) },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.stop.value,
            onValueChange = { model.stop.value = (it.trim()) },
            label = { Text("Stop") },
            isError = model.stop.isError,
            errorText = { Text(model.stop.errorMessage) },
            singleLine = true,
        )

        DateTimeField(
            value = model.entryDateTime.value,
            onValidValueChange = { model.entryDateTime.value = it },
            label = { Text("Entry DateTime") },
            isError = model.entryDateTime.isError,
            errorText = { Text(model.entryDateTime.errorMessage) },
        )

        OutlinedTextField(
            value = model.target.value,
            onValueChange = { model.target.value = it.trim() },
            label = { Text("Target") },
            isError = model.target.isError,
            errorText = { Text(model.target.errorMessage) },
            singleLine = true,
        )

        val focusRequester = remember { FocusRequester() }

        OutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = model.exit.value,
            onValueChange = { model.exit.value = it.trim() },
            label = { Text("Exit") },
            isError = model.exit.isError,
            errorText = { Text(model.exit.errorMessage) },
            singleLine = true,
        )

        if (state.detailModel == null) {

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        DateTimeField(
            value = model.exitDateTime.value,
            onValidValueChange = { model.exitDateTime.value = it },
            label = { Text("Exit DateTime") },
            isError = model.exitDateTime.isError,
            errorText = { Text(model.exitDateTime.errorMessage) },
        )

        val detailModel = state.detailModel

        if (detailModel != null) {

            AnimatedVisibility(
                visible = !state.showDetails,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {

                Button(
                    onClick = state::showDetails,
                ) {

                    Text("Show Details")
                }
            }

            AnimatedVisibility(state.showDetails) {

                DetailForm(detailModel)
            }
        }

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = state::onSaveTrade,
        ) {

            Text("Add")
        }
    }
}

@Composable
private fun DetailForm(
    model: CloseTradeDetailFormModel,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        Divider(Modifier.padding(16.dp))

        OutlinedTextField(
            value = model.maxFavorableExcursion.value,
            onValueChange = { model.maxFavorableExcursion.value = it.trim() },
            label = { Text("Max Favorable Excursion") },
            isError = model.maxFavorableExcursion.isError,
            errorText = { Text(model.maxFavorableExcursion.errorMessage) },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.maxAdverseExcursion.value,
            onValueChange = { model.maxAdverseExcursion.value = it.trim() },
            label = { Text("Max Adverse Excursion") },
            isError = model.maxAdverseExcursion.isError,
            errorText = { Text(model.maxAdverseExcursion.errorMessage) },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Persisted")

            Switch(
                checked = model.persisted,
                onCheckedChange = { model.persisted = it },
            )
        }

        FlowRow(
            mainAxisSpacing = 8.dp,
        ) {

            model.tags.forEach { tag ->

                InputChip(
                    onClick = {},
                    label = { Text(tag) }
                )
            }
        }
    }
}
