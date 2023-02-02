package com.saurabhsandav.core.ui.barreplay.charts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartInfo
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.controls.ListSelectionField
import com.saurabhsandav.core.ui.common.timeframeFromLabel
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import com.saurabhsandav.core.ui.stockchart.StockCharts
import com.saurabhsandav.core.ui.stockchart.ui.StockChartsTabControls
import com.saurabhsandav.core.utils.NIFTY50

@Composable
internal fun ReplayCharts(
    onNewReplay: () -> Unit,
    tabsState: StockChartTabsState,
    chartPageState: ChartPageState,
    chartInfo: ReplayChartInfo,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    onSymbolChange: (String) -> Unit,
    onTimeframeChange: (Timeframe) -> Unit,
) {

    StockCharts(
        pageState = chartPageState,
        tabsState = tabsState,
    ) {

        Legend(chartInfo)

        ListSelectionField(
            items = NIFTY50,
            selection = chartInfo.symbol,
            onSelection = onSymbolChange,
            label = { Text("Ticker") },
        )

        ListSelectionField(
            items = remember { Timeframe.values().map { it.toLabel() } },
            selection = chartInfo.timeframe.toLabel(),
            onSelection = { onTimeframeChange(timeframeFromLabel(it)) },
            label = { Text("Timeframe") },
        )

        Divider()

        StockChartsTabControls(tabsState)

        Divider()

        ReplayControls(
            onNewReplay = onNewReplay,
            onReset = onReset,
            onNext = onNext,
            onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
        )
    }
}

@Composable
private fun Legend(chartInfo: ReplayChartInfo) {

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
