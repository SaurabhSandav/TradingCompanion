package ui.addclosedtradedetailed

import androidx.compose.foundation.layout.*
import androidx.compose.material.Chip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
internal fun CloseTradeDetailedWindow(
    onCloseRequest: () -> Unit,
    formModel: CloseTradeDetailedFormFields.Model,
    onSaveTrade: (CloseTradeDetailedFormFields.Model) -> Unit,
) {

    val windowState = rememberWindowState(
        size = DpSize(width = 600.dp, height = Dp.Unspecified),
    )

    Window(
        onCloseRequest = onCloseRequest,
        state = windowState,
        title = "Close Trade",
        resizable = false,
    ) {

        Row {

            val formScope = rememberFormScope()
            val fields = remember { CloseTradeDetailedFormFields(formScope, formModel) }

            Column(
                modifier = Modifier.weight(1F).padding(16.dp).width(IntrinsicSize.Min),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

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

                val focusRequester = remember { FocusRequester() }

                OutlinedTextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    value = fields.exit.value,
                    onValueChange = fields.exit.onValueChange,
                    label = { Text("Exit") },
                    isError = fields.exit.isError,
                    singleLine = true,
                )

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }

                DateField(
                    value = fields.exitDate.value,
                    onValidValueChange = fields.exitDate.onValueChange,
                    label = { Text("Exit Date") },
                )

                TimeField(
                    value = fields.exitTime.value,
                    onValidValueChange = fields.exitTime.onValueChange,
                    label = { Text("Exit Time") },
                )

                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = { fields.getModelIfValidOrNull()?.let { onSaveTrade(it) } },
                ) {

                    Text("Save")
                }
            }

            Column(
                modifier = Modifier.weight(1F).padding(16.dp).width(IntrinsicSize.Min),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                OutlinedTextField(
                    value = fields.maxFavorableExcursion.value,
                    onValueChange = fields.maxFavorableExcursion.onValueChange,
                    label = { Text("Max Favorable Excursion") },
                    isError = fields.maxFavorableExcursion.isError,
                    singleLine = true,
                )

                OutlinedTextField(
                    value = fields.maxAdverseExcursion.value,
                    onValueChange = fields.maxAdverseExcursion.onValueChange,
                    label = { Text("Max Adverse Excursion") },
                    isError = fields.maxAdverseExcursion.isError,
                    singleLine = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    Text("Persisted")

                    Switch(
                        checked = fields.persisted.value,
                        onCheckedChange = fields.persisted.onCheckedChange,
                    )
                }

                formModel.tags.forEach {

                    Chip({}) {
                        Text(it)
                    }
                }
            }
        }

    }
}
