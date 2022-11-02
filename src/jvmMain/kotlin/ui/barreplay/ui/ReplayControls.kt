package ui.barreplay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.barreplay.model.BarReplayFormFields
import ui.common.controls.ListSelectionField
import ui.common.state
import utils.NIFTY50

@Composable
internal fun ReplayControls(
    fields: BarReplayFormFields,
    enabled: Boolean,
    onNewReplay: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
) {

    Column(
        modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {

        Button(
            onClick = onNewReplay,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        ) {
            Text("New Replay")
        }

        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        ) {
            Text("Reset Replay")
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        ) {
            Text("Next")
        }

        ListSelectionField(
            items = NIFTY50,
            selection = fields.symbol.value,
            onSelection = {
                fields.symbol.onSelectionChange(it)
                onSymbolChange(it)
            },
            label = { Text("Ticker") },
            placeholderText = "Select Ticker...",
            enabled = enabled,
        )

        ListSelectionField(
            items = listOf("5m", "1D"),
            onSelection = {
                fields.timeframe.onSelectionChange(it)
                onTimeframeChange(it)
            },
            selection = fields.timeframe.value,
            label = { Text("Timeframe") },
            placeholderText = "Select Timeframe...",
            enabled = enabled,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Text("Auto next: ")

            var isAutoNextEnabled by state { false }

            Switch(
                checked = isAutoNextEnabled,
                onCheckedChange = {
                    onIsAutoNextEnabledChange(it)
                    isAutoNextEnabled = it
                },
                enabled = enabled,
            )
        }
    }
}
