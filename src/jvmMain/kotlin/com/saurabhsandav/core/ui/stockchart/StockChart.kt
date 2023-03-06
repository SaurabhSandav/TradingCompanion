package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.chart.options.TimeScaleOptions
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.dailySessionStart
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.trading.isLong
import com.saurabhsandav.core.ui.common.chart.ChartDarkModeOptions
import com.saurabhsandav.core.ui.common.chart.ChartLightModeOptions
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.plotter.CandlestickPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.LinePlotter
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.VolumePlotter
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn
import java.math.RoundingMode

internal class StockChart(
    val appModule: AppModule,
    val actualChart: IChartApi,
    onLegendUpdate: (List<String>) -> Unit,
    private val onTitleUpdate: (String) -> Unit,
) {

    private var source: CandleSource? = null

    private val candlestickPlotter = CandlestickPlotter(actualChart)
    private val volumePlotter = VolumePlotter(actualChart)
    private val vwapPlotter = LinePlotter(actualChart, "VWAP", Color(0xFFA500))
    private val ema9Plotter = LinePlotter(actualChart, "EMA (9)")

    val coroutineScope = MainScope()
    var currentParams: Params? by mutableStateOf(null)
    val plotters = mutableStateListOf<SeriesPlotter<*>>()

    init {

        plotters.addAll(
            listOf(
                candlestickPlotter,
                volumePlotter,
                vwapPlotter,
                ema9Plotter,
            )
        )

        actualChart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true)
        )

        // Legend updates
        actualChart.crosshairMove().onEach { params ->
            onLegendUpdate(plotters.map { it.legendText(params) })
        }.launchIn(coroutineScope)

        observerPlotterIsEnabled(PrefKeys.PlotterCandlesEnabled, candlestickPlotter)
        observerPlotterIsEnabled(PrefKeys.PlotterVolumeEnabled, volumePlotter)
        observerPlotterIsEnabled(PrefKeys.PlotterVWAPEnabled, vwapPlotter)
        observerPlotterIsEnabled(PrefKeys.PlotterEMA9Enabled, ema9Plotter)
    }

    fun setCandleSource(source: CandleSource) {

        // Update chart params
        currentParams = Params(source.ticker, source.timeframe)

        // Update chart title
        val chartTitle = "${source.ticker} (${source.timeframe.toLabel()})"
        onTitleUpdate(chartTitle)

        // Update legend title for candles
        candlestickPlotter.name = chartTitle

        // Cancel CoroutineScope for the previous CandleSource
        this@StockChart.source?.coroutineScope?.cancel()

        // Update cached CandleSource
        this@StockChart.source = source

        coroutineScope.launchUnit {

            fun setData() = plotters.forEach { it.setData(source.candleSeries.indices) }

            // Get the candles ready
            source.init(actualChart, candlestickPlotter, onResetData = ::setData)

            // Update chart with live candles
            source.coroutineScope.launch {
                source.candleSeries.live.collect { candle ->
                    plotters.forEach {
                        it.update(source.candleSeries.indexOf(candle))
                    }
                }
            }

            // Setup Indicators
            setupDefaultIndicators(source.candleSeries, source.hasVolume)

            // Set initial data on chart
            setData()
        }
    }

    fun setDarkMode(isDark: Boolean) {
        actualChart.applyOptions(if (isDark) ChartDarkModeOptions else ChartLightModeOptions)
    }

    fun setPlotterIsEnabled(plotter: SeriesPlotter<*>, isEnabled: Boolean) = coroutineScope.launchUnit {

        val prefKey = when (plotter) {
            candlestickPlotter -> PrefKeys.PlotterCandlesEnabled
            volumePlotter -> PrefKeys.PlotterVolumeEnabled
            vwapPlotter -> PrefKeys.PlotterVWAPEnabled
            ema9Plotter -> PrefKeys.PlotterEMA9Enabled
            else -> error("Unknown plotter ${plotter.name}")
        }

        appModule.appPrefs.putBoolean(prefKey, isEnabled)
    }

    private fun setupDefaultIndicators(
        candleSeries: CandleSeries,
        hasVolume: Boolean,
    ) {

        val ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        val vwapIndicator = VWAPIndicator(candleSeries, ::dailySessionStart)

        candlestickPlotter.setDataSource { index ->

            val candle = candleSeries[index]

            CandlestickData(
                time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart()),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )
        }

        ema9Plotter.setDataSource { index ->
            LineData(
                time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                value = ema9Indicator[index].setScale(2, RoundingMode.DOWN),
            )
        }

        if (!hasVolume) {
            volumePlotter.setDataSource(null)
            vwapPlotter.setDataSource(null)
        } else {

            volumePlotter.setDataSource { index ->

                val candle = candleSeries[index]

                HistogramData(
                    time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart()),
                    value = candle.volume,
                    color = when {
                        candle.isLong -> Color(255, 82, 82)
                        else -> Color(0, 150, 136)
                    },
                )
            }

            vwapPlotter.setDataSource { index ->
                LineData(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = vwapIndicator[index].setScale(2, RoundingMode.DOWN),
                )
            }
        }
    }

    private fun observerPlotterIsEnabled(
        prefKey: String,
        plotter: SeriesPlotter<*>,
    ) {
        appModule.appPrefs
            .getBooleanFlow(prefKey, true)
            .onEach(plotter::setIsEnabled)
            .launchIn(coroutineScope)
    }

    fun destroy() {
        coroutineScope.cancel()
        source?.coroutineScope?.cancel()
        plotters.forEach { it.remove() }
        actualChart.remove()
    }

    data class Params(
        val ticker: String,
        val timeframe: Timeframe,
    )
}

fun Instant.offsetTimeForChart(): Long {
    val timeZoneOffset = offsetIn(TimeZone.currentSystemDefault()).totalSeconds
    return epochSeconds + timeZoneOffset
}
