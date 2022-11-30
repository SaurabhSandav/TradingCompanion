package ui.addopentrade

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import ui.common.AppWindow
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.form.rememberFormScope
import utils.NIFTY50

@Composable
internal fun AddOpenTradeWindow(
    state: AddOpenTradeWindowState,
) {

    val windowState = rememberWindowState()

    AppWindow(
        onCloseRequest = state.onCloseRequest,
        state = windowState,
        title = "New Trade",
    ) {

        Box(Modifier.wrapContentSize()) {

            Column(
                modifier = Modifier.padding(16.dp).width(IntrinsicSize.Min),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                val formScope = rememberFormScope()
                val fields = remember { AddOpenTradeFormFields(formScope, state.formModel) }

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

                DateTimeField(
                    value = fields.entryDateTime.value,
                    onValidValueChange = fields.entryDateTime.onValueChange,
                    label = { Text("Entry DateTime") },
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
                    onClick = { fields.getModelIfValidOrNull()?.let(state::onSaveTrade) },
                ) {

                    Text("Add")
                }
            }
        }
    }
}
