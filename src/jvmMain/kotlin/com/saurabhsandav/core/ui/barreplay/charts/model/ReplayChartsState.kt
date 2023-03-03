package com.saurabhsandav.core.ui.barreplay.charts.model

import androidx.compose.runtime.Immutable
import com.saurabhsandav.core.ui.stockchart.StockChart
import com.saurabhsandav.core.ui.stockchart.StockChartsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Immutable
internal data class ReplayChartsState(
    val chartsState: StockChartsState,
    val chartInfo: (StockChart) -> ReplayChartInfo = { ReplayChartInfo() },
)

@Immutable
internal data class ReplayChartInfo(
    val replayTime: Flow<String> = emptyFlow(),
)
