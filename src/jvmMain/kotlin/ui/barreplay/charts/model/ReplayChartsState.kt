package ui.barreplay.charts.model

import androidx.compose.runtime.Immutable
import ui.common.chart.state.ChartPageState

@Immutable
data class ReplayChartsState(
    val chartTabsState: ReplayChartTabsState,
    val chartPageState: ChartPageState,
    val chartInfo: ReplayChartInfo,
)

@Immutable
data class ReplayChartTabsState(
    val tabs: List<TabInfo>,
    val selectedTabIndex: Int,
) {

    @Immutable
    data class TabInfo(
        val id: Int,
        val title: String,
    )
}

@Immutable
data class ReplayChartInfo(
    val symbol: String,
    val timeframe: String,
    val replayTime: String = "",
    val legendValues: LegendValues = LegendValues(),
)

@Immutable
data class LegendValues(
    val open: String = "",
    val high: String = "",
    val low: String = "",
    val close: String = "",
    val volume: String = "",
    val ema9: String = "",
    val vwap: String = "",
)
