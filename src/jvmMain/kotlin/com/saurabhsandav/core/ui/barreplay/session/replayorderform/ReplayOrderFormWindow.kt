package com.saurabhsandav.core.ui.barreplay.session.replayorderform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.form2.isError
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.persistentListOf
import java.util.*

@Composable
internal fun ReplayOrderFormWindow(
    replayOrdersManager: ReplayOrdersManager,
    initialModel: ReplayOrderFormModel.Initial?,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember {
        ReplayOrderFormPresenter(scope, replayOrdersManager, initialModel) { onCloseRequest() }
    }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(size = DpSize(width = 300.dp, height = 550.dp))

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
    ) {

        Box(Modifier.wrapContentSize()) {

            val formModel = state.formModel

            when {
                formModel != null -> ReplayOrderForm(
                    model = formModel,
                    onSaveOrder = { state.onSaveOrder() },
                )

                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun ReplayOrderForm(
    model: ReplayOrderFormModel,
    onSaveOrder: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxHeight()
            .padding(16.dp)
            .width(IntrinsicSize.Min)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {

        val initialFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        OutlinedListSelectionField(
            modifier = Modifier.focusRequester(initialFocusRequester),
            items = remember { persistentListOf(*enumValues<Instrument>()) },
            itemText = {
                it.strValue.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            },
            onSelection = { model.instrumentField.value = it },
            selection = model.instrumentField.value,
            label = { Text("Instrument") },
            isError = model.instrumentField.isError,
            supportingText = model.instrumentField.errorMessage?.let { { Text(it) } },
        )

        OutlinedListSelectionField(
            items = NIFTY50,
            itemText = { it },
            onSelection = { model.tickerField.value = it },
            selection = model.tickerField.value,
            label = { Text("Ticker") },
            isError = model.tickerField.isError,
            supportingText = model.tickerField.errorMessage?.let { { Text(it) } },
        )

        OutlinedTextField(
            value = model.quantityField.value,
            onValueChange = { model.quantityField.value = it.trim() },
            label = { Text("Quantity") },
            isError = model.quantityField.isError,
            supportingText = model.quantityField.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.lotsField.value,
            onValueChange = { model.lotsField.value = it.trim() },
            label = { Text("Lots") },
            isError = model.lotsField.isError,
            supportingText = model.lotsField.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("SELL", color = AppColor.LossRed)

            Switch(
                checked = model.isBuyField.value,
                onCheckedChange = { model.isBuyField.value = it },
            )

            Text("BUY", color = AppColor.ProfitGreen)
        }

        OutlinedTextField(
            value = model.priceField.value,
            onValueChange = { model.priceField.value = it.trim() },
            label = { Text("Price") },
            isError = model.priceField.isError,
            supportingText = model.priceField.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.stop.value,
            onValueChange = { model.stop.value = it.trim() },
            label = { Text("Stop") },
            isError = model.stop.isError,
            supportingText = model.stop.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.target.value,
            onValueChange = { model.target.value = it.trim() },
            label = { Text("Target") },
            isError = model.target.isError,
            supportingText = model.target.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onSaveOrder,
            enabled = model.validator.isValid,
            content = { Text("Add") },
        )
    }
}
