package ui.barreplay.charts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.barreplay.charts.model.ReplayControlsState
import ui.common.controls.ListSelectionField
import ui.common.state
import utils.NIFTY50

@Composable
internal fun ReplayControls(
    state: ReplayControlsState,
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

        Divider()

        ListSelectionField(
            items = NIFTY50,
            selection = state.symbol,
            onSelection = onSymbolChange,
            label = { Text("Change Ticker") },
            enabled = enabled,
        )

        ListSelectionField(
            items = listOf("5m", "1D"),
            onSelection = onTimeframeChange,
            selection = state.timeframe,
            label = { Text("Change Timeframe") },
            enabled = enabled,
        )
    }
}
