package com.saurabhsandav.core.ui.barreplay.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState

@Immutable
data class ReplayChartsState(
    val tabsState: StockChartTabsState,
    val chartPageState: ChartPageState,
    val chartInfo: ReplayChartInfo,
)

@Immutable
data class ReplayChartInfo(
    val ticker: String,
    val timeframe: Timeframe,
    val replayTime: String = "",
)
