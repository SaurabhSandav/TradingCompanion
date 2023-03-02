package com.saurabhsandav.core.ui.barreplay.charts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.barreplay.charts.model.ReplayChartInfo
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import com.saurabhsandav.core.ui.stockchart.StockCharts

@Composable
internal fun ReplayCharts(
    onNewReplay: () -> Unit,
    tabsState: StockChartTabsState,
    chartPageState: ChartPageState,
    chartInfo: ReplayChartInfo,
    onReset: () -> Unit,
    onNext: () -> Unit,
    onIsAutoNextEnabledChange: (Boolean) -> Unit,
    onTickerChange: (String) -> Unit,
    onTimeframeChange: (Timeframe) -> Unit,
) {

    StockCharts(
        pageState = chartPageState,
        stockChart = chartInfo.stockChart,
        onTickerChange = onTickerChange,
        onTimeframeChange = onTimeframeChange,
        tabsState = tabsState,
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {

            Text("Time")

            val replayTime by chartInfo.replayTime.collectAsState("")

            Text(replayTime, textAlign = TextAlign.End)
        }

        ReplayControls(
            onNewReplay = onNewReplay,
            onReset = onReset,
            onNext = onNext,
            onIsAutoNextEnabledChange = onIsAutoNextEnabledChange,
        )
    }
}
