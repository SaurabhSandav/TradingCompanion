package ui.addopentrade

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import ui.common.controls.DateField
import ui.common.controls.ListSelectionField
import ui.common.controls.TimeField
import ui.common.form.rememberFormScope
import utils.NIFTY50

@Composable
internal fun AddOpenTradeWindow(
    onCloseRequest: () -> Unit,
    formModel: AddOpenTradeFormFields.Model,
    onSaveTrade: (AddOpenTradeFormFields.Model) -> Unit,
) {

    val windowState = rememberWindowState(
        size = DpSize(width = 300.dp, height = Dp.Unspecified),
    )

    Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = "New Trade",
        resizable = false,
    ) {

        Column(
            modifier = Modifier.padding(16.dp).width(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            val formScope = rememberFormScope()
            val fields = remember { AddOpenTradeFormFields(formScope, formModel) }

            ListSelectionField(
                items = NIFTY50,
                onSelection = fields.ticker.onSelectionChange,
                selection = fields.ticker.value,
                label = { Text("Ticker") },
                isError = fields.ticker.isError,
            )

            OutlinedTextField(
                value = fields.quantity.value,
                onValueChange = fields.quantity.onValueChange,
                label = { Text("Quantity") },
                isError = fields.quantity.isError,
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("Short")

                Switch(
                    checked = fields.isLong.value,
                    onCheckedChange = fields.isLong.onCheckedChange,
                    colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colors.secondaryVariant,
                        uncheckedTrackColor = MaterialTheme.colors.secondaryVariant,
                        uncheckedTrackAlpha = 0.54f,
                    )
                )

                Text("Long")
            }

            OutlinedTextField(
                value = fields.entry.value,
                onValueChange = fields.entry.onValueChange,
                label = { Text("Entry") },
                isError = fields.entry.isError,
                singleLine = true,
            )

            OutlinedTextField(
                value = fields.stop.value,
                onValueChange = fields.stop.onValueChange,
                label = { Text("Stop") },
                isError = fields.stop.isError,
                singleLine = true,
            )

            DateField(
                value = fields.entryDate.value,
                onValidValueChange = fields.entryDate.onValueChange,
                label = { Text("Entry Date") },
            )

            TimeField(
                value = fields.entryTime.value,
                onValidValueChange = fields.entryTime.onValueChange,
                label = { Text("Entry Time") },
            )

            OutlinedTextField(
                value = fields.target.value,
                onValueChange = fields.target.onValueChange,
                label = { Text("Target") },
                isError = fields.target.isError,
                singleLine = true,
            )

            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { fields.getModelIfValidOrNull()?.let { onSaveTrade(it) } },
            ) {

                Text("Add")
            }
        }
    }
}
