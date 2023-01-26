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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ui.barreplay.charts.model.ReplayChartInfo
import ui.common.TimeframeLabels
import ui.common.controls.ListSelectionField
import ui.common.state
import ui.stockchart.StockChartTabsState
import utils.NIFTY50

@Composable
internal fun ReplayControls(
    chartInfo: ReplayChartInfo,
    onNewReplay: () -> Unit,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    tabsState: StockChartTabsState,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
) {

    Column(
        modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp),
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

            LegendItem("Time", chartInfo.replayTime)

            Divider()

            LegendItem("Open", chartInfo.legendValues.open)

            LegendItem("High", chartInfo.legendValues.high)

            LegendItem("Low", chartInfo.legendValues.low)

            LegendItem("Close", chartInfo.legendValues.close)

            LegendItem("Volume", chartInfo.legendValues.volume)

            LegendItem("EMA (9)", chartInfo.legendValues.ema9)

            LegendItem("VWAP", chartInfo.legendValues.vwap)

            Divider()

            ListSelectionField(
                items = NIFTY50,
                selection = chartInfo.symbol,
                onSelection = onSymbolChange,
                label = { Text("Change Ticker") },
            )

            ListSelectionField(
                items = TimeframeLabels,
                selection = chartInfo.timeframe,
                onSelection = onTimeframeChange,
                label = { Text("Change Timeframe") },
            )

            Divider()
        }

        Column(
            modifier = Modifier.weight(1F),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

            Button(
                onClick = tabsState::moveTabBackward,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Move Tab Backward")
            }

            Button(
                onClick = tabsState::moveTabForward,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Move Tab Forward")
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

@Composable
private fun LegendItem(
    title: String,
    value: String,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {

        Text(title)

        Text(value, textAlign = TextAlign.End)
    }
}
