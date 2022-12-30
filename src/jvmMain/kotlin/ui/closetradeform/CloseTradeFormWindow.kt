package ui.closetradeform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import ui.common.AppWindow
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.flow.FlowRow
import ui.common.form.rememberFormScope
import ui.common.state
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

        val formScope = rememberFormScope()
        val fields = remember { CloseTradeFormFields(formScope, state.model) }

        ListSelectionField(
            items = NIFTY50,
            onSelection = fields.ticker.onSelectionChange,
            selection = fields.ticker.value,
            label = { Text("Ticker") },
            isError = fields.ticker.isError,
        )

        OutlinedTextField(
            value = fields.quantity.value,
            onValueChange = fields.quantity.onValueChange,
            label = { Text("Quantity") },
            isError = fields.quantity.isError,
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Short")

            Switch(
                checked = fields.isLong.value,
                onCheckedChange = fields.isLong.onCheckedChange,
            )

            Text("Long")
        }

        OutlinedTextField(
            value = fields.entry.value,
            onValueChange = fields.entry.onValueChange,
            label = { Text("Entry") },
            isError = fields.entry.isError,
            singleLine = true,
        )

        OutlinedTextField(
            value = fields.stop.value,
            onValueChange = fields.stop.onValueChange,
            label = { Text("Stop") },
            isError = fields.stop.isError,
            singleLine = true,
        )

        DateTimeField(
            value = fields.entryDateTime.value,
            onValidValueChange = fields.entryDateTime.onValueChange,
            label = { Text("Entry DateTime") },
        )

        OutlinedTextField(
            value = fields.target.value,
            onValueChange = fields.target.onValueChange,
            label = { Text("Target") },
            isError = fields.target.isError,
            singleLine = true,
        )

        val focusRequester = remember { FocusRequester() }

        OutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = fields.exit.value,
            onValueChange = fields.exit.onValueChange,
            label = { Text("Exit") },
            isError = fields.exit.isError,
            singleLine = true,
        )

        if (state.detailModel == null) {

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        DateTimeField(
            value = fields.exitDateTime.value,
            onValidValueChange = fields.exitDateTime.onValueChange,
            label = { Text("Exit DateTime") },
        )

        val detailModel = state.detailModel
        val detailFields = remember(detailModel) {
            if (detailModel == null) null else CloseTradeDetailedFormFields(formScope, detailModel)
        }

        if (detailModel != null && detailFields != null) {

            var showDetails by state { false }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {

                Text("Show Details")

                Switch(
                    checked = showDetails,
                    onCheckedChange = { showDetails = it },
                )
            }

            AnimatedVisibility(showDetails) {

                DetailForm(
                    fields = detailFields,
                    tags = detailModel.tags,
                )
            }
        }

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                fields.getModelIfValidOrNull()?.let { model ->
                    state.onSaveTrade(model, detailFields?.getModelIfValidOrNull())
                }
            },
        ) {

            Text("Add")
        }
    }
}

@Composable
private fun DetailForm(
    fields: CloseTradeDetailedFormFields,
    tags: List<String>,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        Divider(Modifier.padding(16.dp))

        OutlinedTextField(
            value = fields.maxFavorableExcursion.value,
            onValueChange = fields.maxFavorableExcursion.onValueChange,
            label = { Text("Max Favorable Excursion") },
            isError = fields.maxFavorableExcursion.isError,
            singleLine = true,
        )

        OutlinedTextField(
            value = fields.maxAdverseExcursion.value,
            onValueChange = fields.maxAdverseExcursion.onValueChange,
            label = { Text("Max Adverse Excursion") },
            isError = fields.maxAdverseExcursion.isError,
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Persisted")

            Switch(
                checked = fields.persisted.value,
                onCheckedChange = fields.persisted.onCheckedChange,
            )
        }

        FlowRow(
            mainAxisSpacing = 8.dp,
        ) {

            tags.forEach { tag ->

                InputChip(
                    onClick = {},
                    label = { Text(tag) }
                )
            }
        }
    }
}
