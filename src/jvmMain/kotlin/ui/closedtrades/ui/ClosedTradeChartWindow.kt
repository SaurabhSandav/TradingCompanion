package ui.closedtrades.ui

import AppDensityFraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import chart.*
import chart.misc.LineWidth
import chart.misc.PriceFormat
import chart.pricescale.PriceScaleMargins
import chart.pricescale.PriceScaleOptions
import chart.series.histogram.HistogramStyleOptions
import chart.series.line.LineStyleOptions
import chart.timescale.TimeScaleOptions
import ui.closedtrades.model.ClosedTradeChartData
import ui.common.ResizableChart

@Composable
internal fun ClosedTradeChartWindow(
    onCloseRequest: () -> Unit,
    chartData: ClosedTradeChartData,
) {

    Window(
        onCloseRequest = onCloseRequest,
        state = rememberWindowState(placement = WindowPlacement.Maximized),
        title = "Chart",
    ) {

        val density = LocalDensity.current

        val newDensity = Density(density.density * AppDensityFraction, density.fontScale)

        CompositionLocalProvider(LocalDensity provides newDensity) {

            ResizableChart(
                options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal))
            ) {
                configure(chartData)
            }
        }
    }
}

private fun IChartApi.configure(
    chartData: ClosedTradeChartData,
) {

    val candleSeries by candlestickSeries()
    val volumeSeries by histogramSeries(
        HistogramStyleOptions(
            priceFormat = PriceFormat.BuiltIn(
                type = PriceFormat.Type.Volume,
            ),
            priceScaleId = "",
        )
    )
    val ema9Series by lineSeries(
        options = LineStyleOptions(
            lineWidth = LineWidth.One,
        ),
    )
    val vwapSeries by lineSeries(
        options = LineStyleOptions(
            lineWidth = LineWidth.One,
            color = Color.Yellow,
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
}
