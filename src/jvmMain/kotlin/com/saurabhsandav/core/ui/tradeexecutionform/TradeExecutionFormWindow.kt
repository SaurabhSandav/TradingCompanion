package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.controls.DatePickerField
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.controls.TimeField
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.AddToTrade
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.NewFromExistingInTrade
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*

@Composable
internal fun TradeExecutionFormWindow(
    profileId: Long,
    formType: TradeExecutionFormType,
    onExecutionSaved: ((executionId: Long) -> Unit)? = null,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        TradeExecutionFormPresenter(scope, profileId, formType, appModule) { id ->
            onExecutionSaved?.invoke(id)
            onCloseRequest()
        }
    }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(
        size = DpSize(width = 300.dp, height = 600.dp),
    )

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
    ) {

        TradeExecutionFormScreen(
            formType = formType,
            formModel = state.formModel,
            onSaveExecution = state.onSaveExecution,
        )
    }
}

@Composable
private fun TradeExecutionFormScreen(
    formType: TradeExecutionFormType,
    formModel: TradeExecutionFormModel?,
    onSaveExecution: () -> Unit,
) {

    Box(Modifier.wrapContentSize()) {

        when {
            formModel != null -> TradeExecutionForm(
                formType = formType,
                model = formModel,
                onSaveExecution = onSaveExecution,
            )

            else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun TradeExecutionForm(
    formType: TradeExecutionFormType,
    model: TradeExecutionFormModel,
    onSaveExecution: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .width(IntrinsicSize.Min)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {

        val isTickerEditable = !(formType is NewFromExistingInTrade || formType is AddToTrade)

        OutlinedListSelectionField(
            items = remember { persistentListOf(*enumValues<Instrument>()) },
            itemText = {
                it.strValue.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            },
            onSelection = { model.instrument.value = it },
            selection = model.instrument.value,
            label = { Text("Instrument") },
            enabled = isTickerEditable,
            isError = model.instrument.isError,
            supportingText = model.instrument.errorMessage?.let { { Text(it) } },
        )

        OutlinedListSelectionField(
            items = NIFTY50,
            itemText = { it },
            onSelection = { model.ticker.value = it },
            selection = model.ticker.value,
            label = { Text("Ticker") },
            enabled = isTickerEditable,
            isError = model.ticker.isError,
            supportingText = model.ticker.errorMessage?.let { { Text(it) } },
        )

        OutlinedTextField(
            value = model.quantity.value,
            onValueChange = { model.quantity.value = it.trim() },
            label = { Text("Quantity") },
            isError = model.quantity.isError,
            supportingText = model.quantity.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        OutlinedTextField(
            value = model.lots.value,
            onValueChange = { model.lots.value = it.trim() },
            label = { Text("Lots") },
            isError = model.lots.isError,
            supportingText = model.lots.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("SELL", color = AppColor.LossRed)

            Switch(
                checked = model.isBuy.value,
                onCheckedChange = { model.isBuy.value = it },
            )

            Text("BUY", color = AppColor.ProfitGreen)
        }

        OutlinedTextField(
            value = model.price.value,
            onValueChange = { model.price.value = it.trim() },
            label = { Text("Price") },
            isError = model.price.isError,
            supportingText = model.price.errorMessage?.let { { Text(it) } },
            singleLine = true,
        )

        DatePickerField(
            value = model.date.value,
            onValidValueChange = { model.date.value = it },
            label = { Text("Entry Date") },
            isError = model.date.isError,
            supportingText = model.date.errorMessage?.let { { Text(it) } },
            yearRange = remember {
                1900..Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
            }
        )

        TimeField(
            value = model.time.value,
            onValidValueChange = { model.time.value = it },
            label = { Text("Entry Time") },
            isError = model.time.isError,
            supportingText = model.time.errorMessage?.let { { Text(it) } },
        )

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onSaveExecution,
        ) {

            Text("Add")
        }
    }
}
