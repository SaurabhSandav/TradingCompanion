package com.saurabhsandav.core.ui.pnlcalculator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.IconButtonWithTooltip
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.app.rememberAppWindowState
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.table.*
import com.saurabhsandav.core.ui.common.table.Column.Width.Weight
import com.saurabhsandav.core.ui.theme.dimens

@Composable
internal fun PNLCalculatorWindow(
    state: PNLCalculatorWindowState,
) {

    val windowState = rememberAppWindowState(
        size = DpSize(width = 1150.dp, height = 600.dp),
        preferredPlacement = WindowPlacement.Floating,
        forcePreferredPlacement = true,
    )

    AppWindow(
        onCloseRequest = state.params.onCloseRequest,
        state = windowState,
        title = "Calculate PNL",
    ) {

        Box(Modifier.wrapContentSize()) {

            when {
                state.isReady -> CalculatorForm(state)
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun CalculatorForm(state: PNLCalculatorWindowState) {

    Row(Modifier.width(1100.dp).fillMaxHeight()) {

        val model = state.model

        Column(
            modifier = Modifier.padding(16.dp).weight(1F).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(
                space = MaterialTheme.dimens.columnVerticalSpacing,
                alignment = Alignment.CenterVertically,
            ),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = MaterialTheme.dimens.rowHorizontalSpacing,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("Short")

                Switch(
                    checked = model.isLongField.value,
                    onCheckedChange = { model.isLongField.value = it },
                    enabled = model.enableModification,
                )

                Text("Long")
            }

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
                enabled = model.enableModification,
            )

            OutlinedTextField(
                value = model.entryField.value,
                onValueChange = { model.entryField.value = it.trim() },
                label = { Text("Entry") },
                isError = model.entryField.isError,
                supportingText = model.entryField.errorMessage?.let { { Text(it) } },
                singleLine = true,
                enabled = model.enableModification,
            )

            OutlinedTextField(
                value = model.exitField.value,
                onValueChange = { model.exitField.value = it.trim() },
                label = { Text("Exit") },
                isError = model.exitField.isError,
                supportingText = model.exitField.errorMessage?.let { { Text(it) } },
                singleLine = true,
            )

            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = model.validator.isValid,
                onClick = state::onCalculate,
            ) {

                Text("Calculate")
            }
        }

        val schema = rememberTableSchema<PNLEntry> {
            addColumn("Side") {
                Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
            }
            addColumnText("Quantity") { it.quantity }
            addColumnText("Entry") { it.entry }
            addColumnText("Exit") { it.exit }
            addColumnText("Breakeven", width = Weight(1.2F)) { it.breakeven }
            addColumn("PNL") {
                Text(it.pnl, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
            }
            addColumnText("Charges") { it.charges }
            addColumn("Net PNL") {
                Text(it.netPNL, color = if (it.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
            }
            addColumn(width = Weight(.5F)) {

                val alpha by animateFloatAsState(if (it.isRemovable) 1F else 0F)

                IconButtonWithTooltip(
                    modifier = Modifier.alpha(alpha),
                    onClick = { state.onRemoveCalculation(it.id) },
                    tooltipText = "Close",
                    enabled = it.isRemovable,
                    content = {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    },
                )
            }
        }

        LazyTable(
            modifier = Modifier.padding(16.dp).width(800.dp).fillMaxHeight(),
            schema = schema,
        ) {

            rows(
                items = model.pnlEntries,
            )
        }
    }
}
