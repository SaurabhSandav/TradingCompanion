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
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.theme.dimens

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

    val windowState = rememberAppWindowState(
        size = DpSize(width = 300.dp, height = 450.dp),
        preferredPlacement = WindowPlacement.Floating,
        forcePreferredPlacement = true,
    )

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
    ) {

        val formModel = state.formModel

        when {
            formModel != null -> ReplayOrderForm(
                ticker = state.ticker,
                model = formModel,
            )

            else -> CircularProgressIndicator(Modifier.fillMaxSize().wrapContentSize())
        }
    }
}

@Composable
private fun ReplayOrderForm(
    ticker: String,
    model: ReplayOrderFormModel,
) {

    Column(
        modifier = Modifier.fillMaxSize()
            .wrapContentWidth()
            .width(IntrinsicSize.Min)
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.dimens.containerPadding),
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.columnVerticalSpacing,
            alignment = Alignment.CenterVertically,
        ),
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
            supportingText = model.quantityField.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {

            val isBuy = model.isBuyField.value

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { model.isBuyField.value = false },
                selected = !isBuy,
                colors = SegmentedButtonDefaults.colors(
                    activeContentColor = AppColor.LossRed,
                    inactiveContentColor = AppColor.LossRed,
                ),
                label = { Text("SELL") },
            )

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { model.isBuyField.value = true },
                selected = isBuy,
                colors = SegmentedButtonDefaults.colors(
                    activeContentColor = AppColor.ProfitGreen,
                    inactiveContentColor = AppColor.ProfitGreen,
                ),
                label = { Text("BUY") },
            )
        }

        OutlinedTextField(
            value = model.priceField.value,
            onValueChange = { model.priceField.value = it.trim() },
            label = { Text("Price") },
            isError = model.priceField.isError,
            supportingText = model.priceField.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        OutlinedTextField(
            value = model.stop.value,
            onValueChange = { model.stop.value = it.trim() },
            label = { Text("Stop") },
            isError = model.stop.isError,
            supportingText = model.stop.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        OutlinedTextField(
            value = model.target.value,
            onValueChange = { model.target.value = it.trim() },
            label = { Text("Target") },
            isError = model.target.isError,
            supportingText = model.target.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = model.validator::submit,
            enabled = model.validator.canSubmit,
            content = { Text("Add") },
        )
    }
}
