package ui.barreplay.launchform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ui.common.OutlinedTextField
import ui.common.TimeframeLabels
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.form.isError
import utils.NIFTY50

@Composable
internal fun ReplayLaunchFormScreen(
    model: ReplayLaunchFormModel,
    onLaunchReplay: () -> Unit,
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {

        Column(
            modifier = Modifier.width(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            ListSelectionField(
                items = TimeframeLabels,
                onSelection = { model.baseTimeframe.value = it },
                selection = model.baseTimeframe.value,
                label = { Text("Base Timeframe") },
                placeholderText = "Select Timeframe...",
                isError = model.baseTimeframe.isError,
                errorText = { Text(model.baseTimeframe.errorMessage) },
            )

            OutlinedTextField(
                value = model.candlesBefore.value,
                onValueChange = { model.candlesBefore.value = it.trim() },
                label = { Text("Candles Before") },
                isError = model.candlesBefore.isError,
                errorText = { Text(model.candlesBefore.errorMessage) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            DateTimeField(
                value = model.replayFrom.value,
                onValidValueChange = { model.replayFrom.value = it },
                label = { Text("Replay From") },
                isError = model.replayFrom.isError,
                errorText = { Text(model.replayFrom.errorMessage) },
            )

            DateTimeField(
                value = model.dataTo.value,
                onValidValueChange = { model.dataTo.value = it },
                label = { Text("Data To") },
                isError = model.dataTo.isError,
                errorText = { Text(model.dataTo.errorMessage) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("OHLC")

                Switch(
                    checked = model.replayFullBar,
                    onCheckedChange = { model.replayFullBar = it },
                )

                Text("Full Bar")
            }

            Divider()

            ListSelectionField(
                items = NIFTY50,
                onSelection = { model.initialSymbol.value = it },
                selection = model.initialSymbol.value,
                label = { Text("Ticker") },
                placeholderText = "Select Ticker...",
                isError = model.initialSymbol.isError,
                errorText = { Text(model.initialSymbol.errorMessage) },
            )

            Divider()

            Button(onClick = onLaunchReplay) {
                Text("Launch")
            }
        }
    }
}
