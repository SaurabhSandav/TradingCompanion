package ui.barreplay.charts.model

import androidx.compose.runtime.Immutable
import ui.common.chart.ChartState

@Immutable
data class ReplayChartsState(
    val chartTabsState: ReplayChartTabsState,
    val chartState: ReplayChartState?,
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
data class ReplayChartState(
    val id: Int,
    val symbol: String,
    val timeframe: String,
    val state: ChartState,
    val data: List<Pair<String, String>>,
)
