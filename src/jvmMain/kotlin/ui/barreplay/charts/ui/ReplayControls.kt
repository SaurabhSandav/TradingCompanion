package ui.barreplay.charts.ui

import androidx.compose.animation.animateContentSize
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
import ui.barreplay.charts.model.ReplayChartState
import ui.common.controls.ListSelectionField
import ui.common.state
import utils.NIFTY50

@Composable
internal fun ReplayControls(
    chartState: ReplayChartState,
    onNewReplay: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    onNewChart: () -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
) {

    Column(
        modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp),
    ) {

        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

            chartState.data.forEach {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {

                    Text(it.first)

                    Text(it.second)
                }
            }

            Divider()

            ListSelectionField(
                items = NIFTY50,
                selection = chartState.symbol,
                onSelection = onSymbolChange,
                label = { Text("Change Ticker") },
            )

            ListSelectionField(
                items = listOf("5M", "1D"),
                onSelection = onTimeframeChange,
                selection = chartState.timeframe,
                label = { Text("Change Timeframe") },
            )

            Divider()
        }

        Column(
            modifier = Modifier.weight(1F),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

            Button(
                onClick = { onNewChart() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("New Chart")
            }

            Button(
                onClick = onNewReplay,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("New Replay")
            }

            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset Replay")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
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
                )
            }
        }
    }
}
