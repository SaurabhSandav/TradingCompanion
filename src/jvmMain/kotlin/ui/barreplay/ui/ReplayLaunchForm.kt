package ui.barreplay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.barreplay.model.BarReplayFormFields
import ui.common.controls.DateTimeField
import ui.common.controls.ListSelectionField
import utils.NIFTY50

@Composable
internal fun ReplayLaunchForm(
    fields: BarReplayFormFields,
    onLaunchReplay: () -> Unit,
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        ListSelectionField(
            items = NIFTY50,
            onSelection = fields.symbol.onSelectionChange,
            selection = fields.symbol.value,
            isError = fields.symbol.isError,
            label = { Text("Ticker") },
            placeholderText = "Select Ticker...",
        )

        ListSelectionField(
            items = listOf("5m", "1D"),
            onSelection = fields.timeframe.onSelectionChange,
            selection = fields.timeframe.value,
            isError = fields.timeframe.isError,
            label = { Text("Timeframe") },
            placeholderText = "Select Timeframe...",
        )

        DateTimeField(
            value = fields.dataFrom.value,
            onValidValueChange = fields.dataFrom.onValueChange,
            label = { Text("Data From") },
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

        Button(onLaunchReplay) {
            Text("Launch")
        }
    }
}
