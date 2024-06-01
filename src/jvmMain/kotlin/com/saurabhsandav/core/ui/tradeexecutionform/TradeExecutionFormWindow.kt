package com.saurabhsandav.core.ui.tradeexecutionform

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
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.controls.DatePickerField
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.controls.TimeField
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.theme.dimens
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionField
import com.saurabhsandav.core.ui.tickerselectiondialog.TickerSelectionType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.*
import com.saurabhsandav.core.utils.NIFTY500
import com.saurabhsandav.core.utils.nowIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.util.*

@Composable
internal fun TradeExecutionFormWindow(
    profileId: ProfileId,
    formType: TradeExecutionFormType,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        appModule.tradeExecutionFormModule(scope).presenter(onCloseRequest, profileId, formType)
    }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        size = DpSize(width = 300.dp, height = 650.dp),
        preferredPlacement = WindowPlacement.Floating,
        forcePreferredPlacement = true,
    )

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
    ) {

        TradeExecutionFormScreen(
            formType = formType,
            formModel = state.formModel,
        )
    }
}

@Composable
private fun TradeExecutionFormScreen(
    formType: TradeExecutionFormType,
    formModel: TradeExecutionFormModel?,
) {

    Box(Modifier.wrapContentSize()) {

        when {
            formModel != null -> TradeExecutionForm(
                formType = formType,
                model = formModel,
            )

            else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun TradeExecutionForm(
    formType: TradeExecutionFormType,
    model: TradeExecutionFormModel,
) {

    Column(
        modifier = Modifier.fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .width(IntrinsicSize.Min)
            .padding(MaterialTheme.dimens.containerPadding),
        verticalArrangement = Arrangement.spacedBy(
            space = MaterialTheme.dimens.columnVerticalSpacing,
            alignment = Alignment.CenterVertically,
        ),
    ) {

        val isTickerEditable = !(formType is NewFromExistingInTrade || formType is AddToTrade || formType is CloseTrade)
        val isSideSelectable = !(formType is AddToTrade || formType is CloseTrade)

        val (instrumentFocusRequester, quantityFocusRequester) = remember { FocusRequester.createRefs() }

        LaunchedEffect(Unit) {
            val requester = if (isTickerEditable) instrumentFocusRequester else quantityFocusRequester
            requester.requestFocus()
        }

        OutlinedListSelectionField(
            modifier = Modifier.focusRequester(instrumentFocusRequester),
            items = remember { enumValues<Instrument>().toList() },
            itemText = {
                it.strValue.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            },
            onSelection = { model.instrumentField.value = it },
            selection = model.instrumentField.value,
            label = { Text("Instrument") },
            enabled = isTickerEditable,
            isError = model.instrumentField.isError,
            supportingText = model.instrumentField.errorMessage?.let { { Text(it) } },
        )

        TickerSelectionField(
            type = TickerSelectionType.Regular,
            tickers = NIFTY500,
            selected = model.tickerField.value,
            onSelect = { model.tickerField.value = it },
            enabled = isTickerEditable,
            isError = model.tickerField.isError,
            supportingText = model.tickerField.errorMessage?.let { { Text(it) } },
        )

        OutlinedTextField(
            modifier = Modifier.focusRequester(quantityFocusRequester),
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
            horizontalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.dimens.rowHorizontalSpacing,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("SELL", color = AppColor.LossRed)

            Switch(
                checked = model.isBuyField.value,
                onCheckedChange = { model.isBuyField.value = it },
                enabled = isSideSelectable,
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

        DatePickerField(
            value = model.dateField.value,
            onValidValueChange = { model.dateField.value = it },
            label = { Text("Entry Date") },
            isError = model.dateField.isError,
            supportingText = model.dateField.errorMessage?.let { { Text(it) } },
            yearRange = remember {
                1900..Clock.System.nowIn(TimeZone.currentSystemDefault()).year
            }
        )

        TimeField(
            value = model.timeField.value,
            onValidValueChange = { model.timeField.value = it },
            label = { Text("Entry Time") },
            isError = model.timeField.isError,
            supportingText = model.timeField.errorMessage?.let { { Text(it) } },
        )

        if (formType is NewSized) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    text = "Add Stop at ${formType.target.toPlainString()}",
                    modifier = Modifier.weight(1F),
                )

                Checkbox(
                    checked = model.addStopField.value,
                    onCheckedChange = { model.addStopField.value = it },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    text = "Add Target at ${formType.target.toPlainString()}",
                    modifier = Modifier.weight(1F),
                )

                Checkbox(
                    checked = model.addTargetField.value,
                    onCheckedChange = { model.addTargetField.value = it },
                )
            }
        }

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = model.validator::submit,
            enabled = model.validator.canSubmit,
            content = { Text("Add") },
        )
    }
}
