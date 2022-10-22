package ui.closedtrades.model

import androidx.compose.runtime.mutableStateListOf
import chart.misc.SeriesMarker
import chart.series.candlestick.CandlestickData
import chart.series.histogram.HistogramData
import chart.series.line.LineData

internal class ClosedTradeChartWindowsManager {

    val windows = mutableStateListOf<ChartWindowState>()

    fun openNewWindow(
        tradeId: Int,
        chartData: ClosedTradeChartData,
    ) {

        windows += ChartWindowState(
            tradeId = tradeId,
            chartData = chartData,
            onCloseRequest = windows::remove,
        )
    }
}

internal class ChartWindowState(
    val tradeId: Int,
    val chartData: ClosedTradeChartData,
    val onCloseRequest: (ChartWindowState) -> Unit,
) {

    fun close() = onCloseRequest(this)
}

data class ClosedTradeChartData(
    val candleData: List<CandlestickData>,
    val volumeData: List<HistogramData>,
    val ema9Data: List<LineData>,
    val vwapData: List<LineData>,
    val visibilityIndexRange: ClosedRange<Int>,
    val markers: List<SeriesMarker>,
)
