package ui.barreplay.launchform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ui.common.TimeframeLabels
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import ui.common.form.rememberFormScope
import utils.NIFTY50

@Composable
internal fun ReplayLaunchFormScreen(
    formModel: ReplayLaunchFormFields.Model,
    onLaunchReplay: (ReplayLaunchFormFields.Model) -> Unit,
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

            val formScope = rememberFormScope()

            val fields = remember {
                ReplayLaunchFormFields(
                    formScope = formScope,
                    initial = formModel,
                )
            }

            ListSelectionField(
                items = TimeframeLabels,
                onSelection = fields.baseTimeframe.onSelectionChange,
                selection = fields.baseTimeframe.value,
                isError = fields.baseTimeframe.isError,
                label = { Text("Base Timeframe") },
                placeholderText = "Select Timeframe...",
            )

            OutlinedTextField(
                value = fields.candlesBefore.value,
                onValueChange = fields.candlesBefore.onValueChange,
                label = { Text("Candles Before") },
                isError = fields.candlesBefore.isError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            DateTimeField(
                value = fields.dataTo.value,
                onValidValueChange = fields.dataTo.onValueChange,
                label = { Text("Data To") },
            )

            DateTimeField(
                value = fields.replayFrom.value,
                onValidValueChange = fields.replayFrom.onValueChange,
                label = { Text("Replay From") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Text("OHLC")

                Switch(
                    checked = fields.replayFullBar.value,
                    onCheckedChange = fields.replayFullBar.onCheckedChange,
                )

                Text("Full Bar")
            }

            Divider()

            ListSelectionField(
                items = NIFTY50,
                onSelection = fields.initialSymbol.onSelectionChange,
                selection = fields.initialSymbol.value,
                isError = fields.initialSymbol.isError,
                label = { Text("Initial Ticker") },
                placeholderText = "Select Ticker...",
            )

            Divider()

            Button(onClick = { fields.getModelIfValidOrNull()?.let(onLaunchReplay) }) {
                Text("Launch")
            }
        }
    }
}
