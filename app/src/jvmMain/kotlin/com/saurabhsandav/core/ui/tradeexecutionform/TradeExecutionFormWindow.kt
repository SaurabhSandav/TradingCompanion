package com.saurabhsandav.core.ui.tradeexecutionform

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.trading.ProfileId
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Form
import com.saurabhsandav.core.ui.common.FormDefaults
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.controls.DatePickerField
import com.saurabhsandav.core.ui.common.controls.OutlinedListSelectionField
import com.saurabhsandav.core.ui.common.controls.TimeField
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.symbolselectiondialog.SymbolSelectionField
import com.saurabhsandav.core.ui.symbolselectiondialog.SymbolSelectionType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormModel
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.AddToTrade
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.CloseTrade
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.NewFromExistingInTrade
import com.saurabhsandav.core.ui.tradeexecutionform.model.TradeExecutionFormType.NewSized
import com.saurabhsandav.core.utils.nowIn
import com.saurabhsandav.trading.core.Instrument
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.Clock

@Composable
internal fun TradeExecutionFormWindow(
    profileId: ProfileId,
    formType: TradeExecutionFormType,
    onCloseRequest: () -> Unit,
) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val presenter = remember {
        appGraph.tradeExecutionFormGraphFactory
            .create(profileId, formType)
            .presenterFactory
            .create(onCloseRequest, scope)
    }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        size = DpSize(width = FormDefaults.PreferredWidth, height = 650.dp),
        preferredPlacement = WindowPlacement.Floating,
        forcePreferredPlacement = true,
    )

    val formModel = state.formModel ?: return

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = state.title,
    ) {

        TradeExecutionForm(
            formType = formType,
            model = formModel,
            onSubmit = state.onSubmit,
        )
    }
}

@Composable
private fun TradeExecutionForm(
    formType: TradeExecutionFormType,
    model: TradeExecutionFormModel,
    onSubmit: () -> Unit,
) {

    Form(
        formModels = listOf(model),
        onSubmit = onSubmit,
    ) {

        val isSymbolEditable = !(formType is NewFromExistingInTrade || formType is AddToTrade || formType is CloseTrade)
        val isSideSelectable = !(formType is AddToTrade || formType is CloseTrade)

        val (instrumentFocusRequester, quantityFocusRequester) = remember { FocusRequester.createRefs() }

        LaunchedEffect(Unit) {
            val requester = if (isSymbolEditable) instrumentFocusRequester else quantityFocusRequester
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
            onSelect = { model.instrumentField.holder.value = it },
            selection = model.instrumentField.value,
            label = { Text("Instrument") },
            enabled = isSymbolEditable,
            isError = model.instrumentField.isError,
            supportingText = model.instrumentField.errorsMessagesAsSupportingText(),
        )

        SymbolSelectionField(
            type = SymbolSelectionType.Regular,
            selected = model.symbolField.value,
            onSelect = { model.symbolField.holder.value = it },
            enabled = isSymbolEditable,
            isError = model.symbolField.isError,
            supportingText = model.symbolField.errorsMessagesAsSupportingText(),
        )

        OutlinedTextField(
            modifier = Modifier.focusRequester(quantityFocusRequester),
            value = model.quantityField.value,
            onValueChange = { model.quantityField.holder.value = it.trim() },
            label = { Text("Quantity") },
            isError = model.quantityField.isError,
            supportingText = model.quantityField.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        OutlinedTextField(
            value = model.lotsField.value,
            onValueChange = { model.lotsField.holder.value = it.trim() },
            label = { Text("Lots") },
            isError = model.lotsField.isError,
            supportingText = model.lotsField.errorsMessagesAsSupportingText(),
            singleLine = true,
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
                enabled = isSideSelectable,
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
                enabled = isSideSelectable,
                label = { Text("BUY") },
            )
        }

        OutlinedTextField(
            value = model.priceField.value,
            onValueChange = { model.priceField.holder.value = it.trim() },
            label = { Text("Price") },
            isError = model.priceField.isError,
            supportingText = model.priceField.errorsMessagesAsSupportingText(),
            singleLine = true,
        )

        DatePickerField(
            value = model.dateField.value,
            onValidValueChange = { model.dateField.holder.value = it },
            label = { Text("Entry Date") },
            isError = model.dateField.isError,
            supportingText = model.dateField.errorsMessagesAsSupportingText(),
            yearRange = remember {
                1900..Clock.System.nowIn(TimeZone.currentSystemDefault()).year
            },
        )

        TimeField(
            value = model.timeField.value,
            onValidValueChange = { model.timeField.holder.value = it },
            label = { Text("Entry Time") },
            isError = model.timeField.isError,
            supportingText = model.timeField.errorsMessagesAsSupportingText(),
            trailingIcon = {

                TextButton(
                    onClick = {
                        model.timeField.holder.value = Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .time
                    },
                ) { Text("NOW") }
            },
        )

        if (formType is NewSized) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    text = "Add Stop at ${formType.target}",
                    modifier = Modifier.weight(1F),
                )

                Checkbox(
                    checked = model.addStopField.value,
                    onCheckedChange = { model.addStopField.holder.value = it },
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text(
                    text = "Add Target at ${formType.target}",
                    modifier = Modifier.weight(1F),
                )

                Checkbox(
                    checked = model.addTargetField.value,
                    onCheckedChange = { model.addTargetField.holder.value = it },
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = validator::submit,
            enabled = validator.canSubmit,
            content = { Text("Add") },
        )
    }
}
