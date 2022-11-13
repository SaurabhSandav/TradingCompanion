package ui.barreplay.charts.model

import androidx.compose.runtime.Immutable
import chart.IChartApi

@Immutable
data class ReplayChartsState(
    val areReplayControlsEnabled: Boolean,
    val controlsState: ReplayControlsState,
    val chartState: ReplayChartState,
)

@Immutable
data class ReplayControlsState(
    val symbol: String,
    val timeframe: String,
)

@Immutable
data class ReplayChartState(
    val chart: IChartApi,
)
