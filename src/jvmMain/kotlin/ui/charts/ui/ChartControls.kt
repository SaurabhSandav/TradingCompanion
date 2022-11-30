package ui.charts.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.charts.model.ChartsState
import ui.common.TimeframeLabels
import ui.common.controls.ListSelectionField
import utils.NIFTY50

@Composable
internal fun ChartControls(
    chartInfo: ChartsState.ChartInfo,
    onNewChart: () -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (String) -> Unit,
) {

    Column(
        modifier = Modifier.width(250.dp).fillMaxHeight().padding(16.dp),
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {

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

            Button(
                onClick = { onNewChart() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("New Chart")
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

        Text(value)
    }
}
