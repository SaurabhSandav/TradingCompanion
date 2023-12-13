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
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.stockchart.StockChartParams

@Composable
internal fun ReplayOrderFormWindow(
    replayOrdersManager: ReplayOrdersManager,
    stockChartParams: StockChartParams,
    initialModel: ReplayOrderFormModel.Initial?,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember {
        ReplayOrderFormPresenter(scope, replayOrdersManager, stockChartParams, initialModel, onCloseRequest)
    }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(size = DpSize(width = 300.dp, height = 450.dp))

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
    ) {

        Box(Modifier.wrapContentSize()) {

            val formModel = state.formModel

            when {
                formModel != null -> ReplayOrderForm(
                    ticker = state.ticker,
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
    ticker: String,
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

        OutlinedTextField(
            value = ticker,
            onValueChange = { },
            label = { Text("Ticker") },
            readOnly = true,
        )

        val initialFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        OutlinedTextField(
            modifier = Modifier.focusRequester(initialFocusRequester),
            value = model.quantityField.value,
            onValueChange = { model.quantityField.value = it.trim() },
            label = { Text("Quantity") },
            isError = model.quantityField.isError,
            supportingText = model.quantityField.errorMessage?.let { { Text(it) } },
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
