package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.foundation.layout.*
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
import com.saurabhsandav.core.ui.common.controls.DateTimeField
import com.saurabhsandav.core.ui.common.controls.ListSelectionField
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.optionalContent
import com.saurabhsandav.core.ui.tradeexecutionform.model.OrderFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.OrderFormType
import com.saurabhsandav.core.utils.NIFTY50
import kotlinx.collections.immutable.toImmutableList
import java.util.*

@Composable
internal fun OrderFormWindow(
    profileId: Long,
    formType: OrderFormType,
    onOrderSaved: ((orderId: Long) -> Unit)? = null,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appModule = LocalAppModule.current
    val presenter = remember {
        OrderFormPresenter(scope, profileId, formType, appModule) { id ->
            onOrderSaved?.invoke(id)
            onCloseRequest()
        }
    }
    val state by presenter.state.collectAsState()

    val windowState = rememberWindowState(size = DpSize(width = 300.dp, height = 550.dp))

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
        resizable = false,
    ) {

        Box(Modifier.wrapContentSize()) {

            val formModel = state.formModel

            when {
                formModel != null -> OrderForm(
                    model = formModel,
                    onSaveOrder = { state.onSaveOrder() },
                )

                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun OrderForm(
    model: OrderFormModel,
    onSaveOrder: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxHeight().padding(16.dp).width(IntrinsicSize.Min),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {

        ListSelectionField(
            items = remember {
                enumValues<Instrument>().map { instrument ->
                    instrument.strValue.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                }.toImmutableList()
            },
            onSelection = { model.instrument.value = it },
            selection = model.instrument.value,
            label = { Text("Instrument") },
            isError = model.instrument.isError,
            supportingText = optionalContent(model.instrument.errorMessage) { Text(it) },
        )

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

        OutlinedTextField(
            value = model.lots.value,
            onValueChange = { model.lots.value = it.trim() },
            label = { Text("Lots") },
            isError = model.lots.isError,
            supportingText = optionalContent(model.lots.errorMessage) { Text(it) },
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
            supportingText = optionalContent(model.price.errorMessage) { Text(it) },
            singleLine = true,
        )

        DateTimeField(
            value = model.timestamp.value,
            onValidValueChange = { model.timestamp.value = it },
            label = { Text("Entry DateTime") },
            isError = model.timestamp.isError,
            supportingText = optionalContent(model.timestamp.errorMessage) { Text(it) },
        )

        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onSaveOrder,
        ) {

            Text("Add")
        }
    }
}
