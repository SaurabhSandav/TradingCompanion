package ui.pnlcalculator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import optionalContent
import ui.common.AppColor
import ui.common.OutlinedTextField
import ui.common.app.AppWindow
import ui.common.form.isError
import ui.common.table.*

@Composable
internal fun PNLCalculatorWindow(
    state: PNLCalculatorWindowState,
) {

    val windowState = rememberWindowState()

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

    Row(Modifier.width(800.dp).fillMaxHeight()) {

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
                    checked = model.isLong.value,
                    onCheckedChange = { model.isLong.value = it },
                    enabled = model.enableModification,
                )

                Text("Long")
            }

            OutlinedTextField(
                value = model.quantity.value,
                onValueChange = { model.quantity.value = it.trim() },
                label = { Text("Quantity") },
                isError = model.quantity.isError,
                errorText = optionalContent(model.quantity.errorMessage) { Text(it) },
                singleLine = true,
                enabled = model.enableModification,
            )

            OutlinedTextField(
                value = model.entry.value,
                onValueChange = { model.entry.value = it.trim() },
                label = { Text("Entry") },
                isError = model.entry.isError,
                errorText = optionalContent(model.entry.errorMessage) { Text(it) },
                singleLine = true,
                enabled = model.enableModification,
            )

            OutlinedTextField(
                value = model.exit.value,
                onValueChange = { model.exit.value = it.trim() },
                label = { Text("Exit") },
                isError = model.exit.isError,
                errorText = optionalContent(model.exit.errorMessage) { Text(it) },
                singleLine = true,
            )

            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = state::onCalculate,
            ) {

                Text("Calculate")
            }
        }

        Column(
            modifier = Modifier.padding(16.dp).width(400.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {

            val schema = rememberTableSchema<PNLEntry> {
                addColumnText("Price", span = 2F) { it.price }
                addColumn("PNL") {
                    Text(it.pnl, color = if (it.isProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                addColumn("Net PNL") {
                    Text(it.netPNL, color = if (it.isNetProfitable) AppColor.ProfitGreen else AppColor.LossRed)
                }
                addColumn("", span = .5F) {

                    val alpha by animateFloatAsState(if (it.isRemovable) 1F else 0F)

                    IconButton(
                        onClick = { state.onRemoveCalculation(it.price) },
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
