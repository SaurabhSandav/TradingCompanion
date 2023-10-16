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
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.ui.common.AppColor
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.form.isError
import com.saurabhsandav.core.ui.common.table.*

@Composable
internal fun PNLCalculatorWindow(
    state: PNLCalculatorWindowState,
) {

    val windowState = rememberWindowState(size = DpSize(width = 1150.dp, height = 600.dp))

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
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
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

        Column(
            modifier = Modifier.padding(16.dp).width(800.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {

            val schema = rememberTableSchema<PNLEntry> {
                addColumn("Side") {
                    Text(it.side, color = if (it.side == "LONG") AppColor.ProfitGreen else AppColor.LossRed)
                }
                addColumnText("Quantity") { it.quantity }
                addColumnText("Entry") { it.entry }
                addColumnText("Exit") { it.exit }
                addColumnText("Breakeven") { it.breakeven }
                addColumn("PNL") {
                    Text(it.pnl, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                addColumnText("Charges") { it.charges }
                addColumn("Net PNL") {
                    Text(it.netPNL, color = if (it.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                addColumn("", span = .5F) {

                    val alpha by animateFloatAsState(if (it.isRemovable) 1F else 0F)

                    IconButton(
                        onClick = { state.onRemoveCalculation(it.id) },
                        modifier = Modifier.alpha(alpha),
                        enabled = it.isRemovable,
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            LazyTable(
                schema = schema,
            ) {

                rows(
                    items = model.pnlEntries,
                )
            }
        }
    }
}
