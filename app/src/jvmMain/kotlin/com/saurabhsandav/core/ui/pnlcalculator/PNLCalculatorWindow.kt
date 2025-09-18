package com.saurabhsandav.core.ui.pnlcalculator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.LocalAppGraph
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.Form
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.errorsMessagesAsSupportingText
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.table.LazyTable
import com.saurabhsandav.core.ui.common.table.SimpleHeader
import com.saurabhsandav.core.ui.common.table.SimpleRow
import com.saurabhsandav.core.ui.common.table.TableCell.Width.Weight
import com.saurabhsandav.core.ui.common.table.TableSchema
import com.saurabhsandav.core.ui.common.table.content
import com.saurabhsandav.core.ui.common.table.text
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorEvent.Calculate
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorEvent.RemoveCalculation
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLCalculatorFormModel
import com.saurabhsandav.core.ui.pnlcalculator.model.PNLEntry
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun PNLCalculatorWindow(onCloseRequest: () -> Unit) {

    val scope = rememberCoroutineScope()
    val appGraph = LocalAppGraph.current
    val graph = remember { appGraph.pnlCalculatorGraphFactory.create() }
    val presenter = remember { graph.presenterFactory.create(scope) }
    val state by presenter.state.collectAsState()

    val windowState = rememberAppWindowState(
        size = DpSize(width = 1150.dp, height = 600.dp),
        preferredPlacement = WindowPlacement.Floating,
        forcePreferredPlacement = true,
    )

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = "Calculate PNL",
    ) {

        CalculatorForm(
            formModel = state.formModel,
            pnlEntries = state.pnlEntries,
            onCalculate = { state.eventSink(Calculate) },
            onRemoveCalculation = { id -> state.eventSink(RemoveCalculation(id)) },
        )
    }
}

@Composable
private fun CalculatorForm(
    formModel: PNLCalculatorFormModel,
    pnlEntries: List<PNLEntry>,
    onCalculate: () -> Unit,
    onRemoveCalculation: (Int) -> Unit,
) {

    Row(Modifier.width(1100.dp).fillMaxHeight()) {

        Form(
            modifier = Modifier
                .weight(1F)
                .fillMaxHeight(),
            formModels = listOf(formModel),
            onSubmit = onCalculate,
        ) {

            SingleChoiceSegmentedButtonRow {

                val isLong = formModel.isLongField.value

                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    onClick = { formModel.isLongField.holder.value = false },
                    selected = !isLong,
                    colors = SegmentedButtonDefaults.colors(
                        activeContentColor = AppColor.LossRed,
                        inactiveContentColor = AppColor.LossRed,
                    ),
                    label = { Text("Short") },
                )

                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    onClick = { formModel.isLongField.holder.value = true },
                    selected = isLong,
                    colors = SegmentedButtonDefaults.colors(
                        activeContentColor = AppColor.ProfitGreen,
                        inactiveContentColor = AppColor.ProfitGreen,
                    ),
                    label = { Text("Long") },
                )
            }

            val initialFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) { initialFocusRequester.requestFocus() }

            OutlinedTextField(
                modifier = Modifier.focusRequester(initialFocusRequester),
                value = formModel.quantityField.value,
                onValueChange = { formModel.quantityField.holder.value = it.trim() },
                label = { Text("Quantity") },
                isError = formModel.quantityField.isError,
                supportingText = formModel.quantityField.errorsMessagesAsSupportingText(),
                singleLine = true,
                enabled = formModel.enableModification,
            )

            OutlinedTextField(
                value = formModel.entryField.value,
                onValueChange = { formModel.entryField.holder.value = it.trim() },
                label = { Text("Entry") },
                isError = formModel.entryField.isError,
                supportingText = formModel.entryField.errorsMessagesAsSupportingText(),
                singleLine = true,
                enabled = formModel.enableModification,
            )

            OutlinedTextField(
                value = formModel.exitField.value,
                onValueChange = { formModel.exitField.holder.value = it.trim() },
                label = { Text("Exit") },
                isError = formModel.exitField.isError,
                supportingText = formModel.exitField.errorsMessagesAsSupportingText(),
                singleLine = true,
            )

            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = validator::submit,
                enabled = validator.canSubmit,
            ) {

                Text("Calculate")
            }
        }

        LazyTable(
            modifier = Modifier.padding(MaterialTheme.dimens.containerPadding).width(800.dp).fillMaxHeight(),
            headerContent = {

                PNLTableSchema.SimpleHeader {
                    side.text { "Side" }
                    quantity.text { "Quantity" }
                    entry.text { "Entry" }
                    exit.text { "Exit" }
                    breakeven.text { "Breakeven" }
                    pnl.text { "PNL" }
                    charges.text { "Charges" }
                    netPnl.text { "Net PNL" }
                }
            },
        ) {

            items(
                items = pnlEntries,
            ) { item ->

                PNLTableSchema.SimpleRow(Modifier.animateItem()) {
                    side.content {
                        Text(item.side, color = if (item.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                    }
                    quantity.text { item.quantity }
                    entry.text { item.entry }
                    exit.text { item.exit }
                    breakeven.text { item.breakeven }
                    pnl.content {
                        Text(item.pnl, color = if (item.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                    }
                    charges.text { item.charges }
                    netPnl.content {
                        Text(item.netPNL, color = if (item.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                    }
                    close.content {

                        val alpha by animateFloatAsState(if (item.isRemovable) 1F else 0F)

                        IconButtonWithTooltip(
                            modifier = Modifier.alpha(alpha),
                            onClick = { onRemoveCalculation(item.id) },
                            tooltipText = "Close",
                            enabled = item.isRemovable,
                            content = {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            },
                        )
                    }
                }
            }
        }
    }
}

private object PNLTableSchema : TableSchema() {

    val side = cell()
    val quantity = cell()
    val entry = cell()
    val exit = cell()
    val breakeven = cell(Weight(1.2F))
    val pnl = cell()
    val charges = cell()
    val netPnl = cell()
    val close = cell(Weight(.5F))
}
