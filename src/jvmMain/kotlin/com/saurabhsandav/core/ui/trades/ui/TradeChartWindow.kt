package com.saurabhsandav.core.ui.trades.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.saurabhsandav.core.LocalAppModule
import com.saurabhsandav.core.chart.*
import com.saurabhsandav.core.chart.PriceScaleOptions.PriceScaleMargins
import com.saurabhsandav.core.chart.options.CandlestickStyleOptions
import com.saurabhsandav.core.chart.options.ChartOptions.CrosshairOptions
import com.saurabhsandav.core.chart.options.ChartOptions.CrosshairOptions.CrosshairMode
import com.saurabhsandav.core.chart.options.HistogramStyleOptions
import com.saurabhsandav.core.chart.options.LineStyleOptions
import com.saurabhsandav.core.chart.options.TimeScaleOptions
import com.saurabhsandav.core.chart.options.common.LineWidth
import com.saurabhsandav.core.chart.options.common.PriceFormat
import com.saurabhsandav.core.ui.common.app.AppWindow
import com.saurabhsandav.core.ui.common.chart.ChartPage
import com.saurabhsandav.core.ui.common.chart.state.ChartPageState
import com.saurabhsandav.core.ui.common.chart.themedChartOptions
import com.saurabhsandav.core.ui.trades.model.TradeChartData

@Composable
internal fun TradeChartWindow(
    onCloseRequest: () -> Unit,
    chartData: TradeChartData,
) {

    AppWindow(
        onCloseRequest = onCloseRequest,
        state = rememberWindowState(placement = WindowPlacement.Maximized),
        title = "Trade Chart",
    ) {

        val themedOptions = themedChartOptions()
        val coroutineScope = rememberCoroutineScope()

        val appModule = LocalAppModule.current

        val chart = remember {
            createChart(options = themedOptions.copy(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
        }

        val chartPageState = remember {
            ChartPageState(
                coroutineScope = coroutineScope,
                webViewState = appModule.webViewStateProvider(),
                chart = chart,
            )
        }

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
    chartData: TradeChartData,
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
}
