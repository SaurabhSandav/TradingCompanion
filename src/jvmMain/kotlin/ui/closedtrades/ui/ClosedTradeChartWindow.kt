package ui.closedtrades.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import chart.*
import chart.options.*
import chart.options.common.LineWidth
import chart.options.common.PriceFormat
import ui.closedtrades.model.ClosedTradeChartData
import ui.common.AppWindow
import ui.common.chart.ChartPage
import ui.common.chart.state.ChartPageState
import ui.common.chart.themedChartOptions

@Composable
internal fun ClosedTradeChartWindow(
    onCloseRequest: () -> Unit,
    chartData: ClosedTradeChartData,
) {

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = rememberWindowState(placement = WindowPlacement.Maximized),
        title = "Trade Chart",
    ) {

        val themedOptions = themedChartOptions()
        val coroutineScope = rememberCoroutineScope()

        val chart = remember {
            createChart(options = themedOptions.copy(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
        }

        val chartPageState = remember { ChartPageState(coroutineScope, chart) }

        ChartPage(chartPageState)

        LaunchedEffect(chart) {
            chart.configure(chartData)
        }

        LaunchedEffect(themedOptions) {
            chart.applyOptions(themedOptions)
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

    timeScale.setVisibleLogicalRange(
        from = chartData.visibilityIndexRange.start.toFloat(),
        to = chartData.visibilityIndexRange.endInclusive.toFloat(),
    )

    candleSeries.setMarkers(chartData.markers)

    chartData.priceLines.forEach(candleSeries::createPriceLine)
}
