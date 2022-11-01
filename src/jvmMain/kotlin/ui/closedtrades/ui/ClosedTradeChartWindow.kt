package ui.closedtrades.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import chart.*
import chart.options.*
import chart.options.common.LineWidth
import chart.options.common.PriceFormat
import ui.closedtrades.model.ClosedTradeChartData
import ui.common.AppWindow
import ui.common.ResizableChart

@Composable
internal fun ClosedTradeChartWindow(
    onCloseRequest: () -> Unit,
    chartData: ClosedTradeChartData,
) {

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = rememberWindowState(placement = WindowPlacement.Maximized),
        title = "Chart",
    ) {

        val chart = remember {
            createChart(ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
        }

        ResizableChart(chart)

        LaunchedEffect(chart) {
            chart.configure(chartData)
        }
    }
}

private fun IChartApi.configure(
    chartData: ClosedTradeChartData,
) {

    val candleSeries by candlestickSeries(
        options = CandlestickStyleOptions(
            lastValueVisible = false,
        ),
    )
    val volumeSeries by histogramSeries(
        HistogramStyleOptions(
            lastValueVisible = false,
            priceFormat = PriceFormat.BuiltIn(
                type = PriceFormat.Type.Volume,
            ),
            priceScaleId = "",
            priceLineVisible = false,
        )
    )
    val ema9Series by lineSeries(
        options = LineStyleOptions(
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        ),
    )
    val vwapSeries by lineSeries(
        options = LineStyleOptions(
            color = Color.Yellow,
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        ),
    )

    volumeSeries.priceScale.applyOptions(
        PriceScaleOptions(
            scaleMargins = PriceScaleMargins(
                top = 0.8,
                bottom = 0,
            )
        )
    )

    timeScale.applyOptions(
        TimeScaleOptions(timeVisible = true)
    )

    candleSeries.setData(chartData.candleData)
    volumeSeries.setData(chartData.volumeData)
    ema9Series.setData(chartData.ema9Data)
    vwapSeries.setData(chartData.vwapData)

    timeScale.setVisibleLogicalRange(chartData.visibilityIndexRange.start, chartData.visibilityIndexRange.endInclusive)

    candleSeries.setMarkers(chartData.markers)
}
