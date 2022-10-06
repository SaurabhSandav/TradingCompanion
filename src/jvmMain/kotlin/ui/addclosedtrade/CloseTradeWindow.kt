package ui.addclosedtrade

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import ui.addopentrade.AddOpenTradeFormState
import ui.common.controls.DateField
import ui.common.controls.ListSelectionField
import ui.common.controls.TimeField
import ui.common.form.rememberFormScope

@Composable
internal fun CloseTradeWindow(
    onCloseRequest: () -> Unit,
    openTradeFormModel: AddOpenTradeFormState.Model,
    onSaveTrade: (CloseTradeFormState.Model) -> Unit,
) {

    val windowState = rememberWindowState(
        size = DpSize(width = 300.dp, height = Dp.Unspecified),
    )

    Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = "Close Trade",
        resizable = false,
    ) {

        Column(
            modifier = Modifier.padding(16.dp).width(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            val formScope = rememberFormScope()
            val formState = remember { CloseTradeFormState(formScope, openTradeFormModel) }

            ListSelectionField(
                items = emptyList(),
                onSelection = {},
                selection = openTradeFormModel.ticker,
                label = { Text("Ticker") },
                enabled = false,
            )

            OutlinedTextField(
                value = openTradeFormModel.quantity,
                onValueChange = {},
                label = { Text("Quantity") },
                enabled = false,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("Short")

                Switch(
                    checked = openTradeFormModel.isLong,
                    onCheckedChange = { },
                    enabled = false,
                )

                Text("Long")
            }

            OutlinedTextField(
                value = openTradeFormModel.entry,
                onValueChange = {},
                label = { Text("Entry") },
                enabled = false,
            )

            OutlinedTextField(
                value = openTradeFormModel.stop,
                onValueChange = {},
                label = { Text("Stop") },
                enabled = false,
            )

            DateField(
                value = openTradeFormModel.entryDateTime.date,
                onValidValueChange = { },
                label = { Text("Entry Date") },
                enabled = false,
            )

            TimeField(
                value = openTradeFormModel.entryDateTime.time,
                onValidValueChange = {},
                label = { Text("Entry Time") },
                enabled = false,
            )

            OutlinedTextField(
                value = openTradeFormModel.target,
                onValueChange = {},
                label = { Text("Target") },
                enabled = false,
            )

            OutlinedTextField(
                value = formState.exit.value,
                onValueChange = formState.exit.onValueChange,
                label = { Text("Exit") },
                isError = formState.exit.isError,
                singleLine = true,
            )

            DateField(
                value = formState.exitDate.value,
                onValidValueChange = formState.exitDate.onValueChange,
                label = { Text("Exit Date") },
            )

            TimeField(
                value = formState.exitTime.value,
                onValidValueChange = formState.exitTime.onValueChange,
                label = { Text("Exit Time") },
            )

            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { formState.getModelIfValidOrNull()?.let { onSaveTrade(it) } },
            ) {

                Text("Add")
            }
        }
    }
}
