package com.saurabhsandav.core.ui.barreplay.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartTabsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Immutable
internal data class ReplayChartsState(
    val tabsState: StockChartTabsState,
    val chartPageState: ChartPageState,
    val chartInfo: ReplayChartInfo,
)

@Immutable
internal data class ReplayChartInfo(
    val stockChart: StockChart? = null,
    val replayTime: Flow<String> = emptyFlow(),
)
