package com.saurabhsandav.core.ui.barreplay.session.replayorderform

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.ui.barreplay.session.ReplayOrdersManager
import com.saurabhsandav.core.ui.barreplay.session.replayorderform.model.ReplayOrderFormModel
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Form
import com.saurabhsandav.core.ui.common.FormDefaults
import com.saurabhsandav.core.ui.common.OutlinedTextBox
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.trim
import com.saurabhsandav.core.ui.stockchart.StockChartParams

@Composable
internal fun ReplayOrderFormWindow(
    replayOrdersManager: ReplayOrdersManager,
    stockChartParams: StockChartParams,
    initialModel: ReplayOrderFormModel,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val presenter = remember {
        ReplayOrderFormPresenter(scope, replayOrdersManager, stockChartParams, initialModel, onCloseRequest)
    }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        size = DpSize(width = FormDefaults.PreferredWidth, height = 580.dp),
        preferredPlacement = WindowPlacement.Floating,
        forcePreferredPlacement = true,
    )

    val formModel = state.formModel ?: return

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
    ) {

        ReplayOrderForm(
            ticker = state.ticker,
            model = formModel,
            onSubmit = state.onSubmit,
        )
    }
}

@Composable
private fun ReplayOrderForm(
    ticker: String,
    model: ReplayOrderFormModel,
    onSubmit: () -> Unit,
) {

    Form(
        formModels = listOf(model),
        onSubmit = onSubmit,
    ) {

        OutlinedTextBox(
            value = ticker,
            label = { Text("Symbol") },
        )

        val initialFocusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

        OutlinedTextField(
            modifier = Modifier.focusRequester(initialFocusRequester),
            state = model.quantityField.holder,
            inputTransformation = InputTransformation.trim(),
            label = { Text("Quantity") },
            isError = model.quantityField.isError,
            supportingText = model.quantityField.errorsMessagesAsSupportingText(),
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        OutlinedTextField(
            state = model.lotsField.holder,
            inputTransformation = InputTransformation.trim(),
            label = { Text("Lots") },
            isError = model.lotsField.isError,
            supportingText = model.lotsField.errorsMessagesAsSupportingText(),
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {

            val isBuy = model.isBuyField.value

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                onClick = { model.isBuyField.holder.value = false },
                selected = !isBuy,
                colors = SegmentedButtonDefaults.colors(
                    activeContentColor = AppColor.LossRed,
                    inactiveContentColor = AppColor.LossRed,
                ),
                label = { Text("SELL") },
            )

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                onClick = { model.isBuyField.holder.value = true },
                selected = isBuy,
                colors = SegmentedButtonDefaults.colors(
                    activeContentColor = AppColor.ProfitGreen,
                    inactiveContentColor = AppColor.ProfitGreen,
                ),
                label = { Text("BUY") },
            )
        }

        OutlinedTextField(
            state = model.priceField.holder,
            inputTransformation = InputTransformation.trim(),
            label = { Text("Price") },
            isError = model.priceField.isError,
            supportingText = model.priceField.errorsMessagesAsSupportingText(),
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        OutlinedTextField(
            state = model.stop.holder,
            inputTransformation = InputTransformation.trim(),
            label = { Text("Stop") },
            isError = model.stop.isError,
            supportingText = model.stop.errorsMessagesAsSupportingText(),
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        OutlinedTextField(
            state = model.target.holder,
            inputTransformation = InputTransformation.trim(),
            label = { Text("Target") },
            isError = model.target.isError,
            supportingText = model.target.errorsMessagesAsSupportingText(),
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = validator::submit,
            enabled = validator.canSubmit,
            content = { Text("Add") },
        )
    }
}
