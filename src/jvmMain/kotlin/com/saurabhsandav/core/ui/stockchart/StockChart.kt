package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.*
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
import com.saurabhsandav.core.trading.indicator.SMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.trading.isLong
import com.saurabhsandav.core.ui.common.chart.ChartDarkModeOptions
import com.saurabhsandav.core.ui.common.chart.ChartLightModeOptions
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.visibleLogicalRangeChange
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.plotter.CandlestickPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.LinePlotter
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.VolumePlotter
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import java.math.RoundingMode

internal class StockChart(
    val appModule: AppModule,
    val actualChart: IChartApi,
    onLegendUpdate: (List<String>) -> Unit,
    private val onTitleUpdate: (String) -> Unit,
) {

    var source: CandleSource? = null
        private set
    private var sourceCoroutineScope = MainScope()

    private val candlestickPlotter = CandlestickPlotter(actualChart)
    private val volumePlotter = VolumePlotter(actualChart)
    private val vwapPlotter = LinePlotter(actualChart, "VWAP", Color(0xFFA500))
    private val ema9Plotter = LinePlotter(actualChart, "EMA (9)")
    private val sma50Plotter = LinePlotter(actualChart, "SMA (50)", Color(0x0AB210))
    private val sma100Plotter = LinePlotter(actualChart, "SMA (100)", Color(0xB05F10))
    private val sma200Plotter = LinePlotter(actualChart, "SMA (200)", Color(0xB00C10))

    val coroutineScope = MainScope()
    var currentParams: Params? by mutableStateOf(null)
    val plotters = mutableStateListOf<SeriesPlotter<*>>()
    var markersAreEnabled by mutableStateOf(false)

    init {

        plotters.addAll(
            listOf(
                candlestickPlotter,
                volumePlotter,
                vwapPlotter,
                ema9Plotter,
                sma50Plotter,
                sma100Plotter,
                sma200Plotter,
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
        observerPlotterIsEnabled(PrefKeys.PlotterSMA50Enabled, sma50Plotter)
        observerPlotterIsEnabled(PrefKeys.PlotterSMA100Enabled, sma100Plotter)
        observerPlotterIsEnabled(PrefKeys.PlotterSMA200Enabled, sma200Plotter)
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
        sourceCoroutineScope.cancel()

        // Update cached CandleSource
        this@StockChart.source = source
        sourceCoroutineScope = MainScope()

        coroutineScope.launchUnit {

            fun setData() = plotters.forEach { it.setData(source.candleSeries.indices) }

            // Get the candles ready
            source.onLoad()

            // Load before/after candles if needed
            actualChart.timeScale
                .visibleLogicalRangeChange()
                .conflate()
                .filterNotNull()
                .onEach { logicalRange ->

                    val barsInfo = candlestickPlotter.series?.barsInLogicalRange(logicalRange) ?: return@onEach

                    when {
                        // Load more historical data if there are less than 100 bars to the left of the visible area
                        barsInfo.barsBefore < 100 && source.onLoadBefore() -> setData()

                        // Load more new data if there are less than 100 bars to the right of the visible area
                        barsInfo.barsAfter < 100 && source.onLoadAfter() -> setData()
                    }
                }
                .launchIn(sourceCoroutineScope)

            // Update chart with live candles
            source.candleSeries
                .live
                .onEach { candle ->
                    plotters.forEach {
                        it.update(source.candleSeries.indexOf(candle))
                    }
                }
                .launchIn(sourceCoroutineScope)

            // Setup Indicators
            setupDefaultIndicators(source.candleSeries, source.hasVolume)

            // Set initial data on chart
            setData()

            // Set markers
            snapshotFlow { markersAreEnabled }
                .flatMapLatest { markersAreEnabled ->
                    when {
                        markersAreEnabled -> source.candleMarkers
                        else -> flowOf(emptyList())
                    }
                }
                .onEach(candlestickPlotter::setMarkers)
                .launchIn(sourceCoroutineScope)
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
            sma50Plotter -> PrefKeys.PlotterSMA50Enabled
            sma100Plotter -> PrefKeys.PlotterSMA100Enabled
            sma200Plotter -> PrefKeys.PlotterSMA200Enabled
            else -> error("Unknown plotter ${plotter.name}")
        }

        appModule.appPrefs.putBoolean(prefKey, isEnabled)
    }

    fun loadDateTime(dateTime: LocalDateTime) = sourceCoroutineScope.launch {

        val source = checkNotNull(source) { "Source not set on chart" }

        // Load candles
        if (source.onLoadDateTime(dateTime)) plotters.forEach { it.setData(source.candleSeries.indices) }
    }

    fun goToDateTime(dateTime: LocalDateTime?) {

        val source = checkNotNull(source) { "Source not set on chart" }

        val candleIndex = when (dateTime) {
            // If datetime is not provided, go to latest candle
            null -> source.candleSeries.lastIndex
            // Find candle index
            else -> {
                val instant = dateTime.toInstant(TimeZone.currentSystemDefault())
                val candleIndex = source.candleSeries.indexOfFirst { it.openInstant > instant }
                // If datetime is not in current candle range, navigate to the latest candles
                if (candleIndex != -1) candleIndex else source.candleSeries.lastIndex
            }
        }

        // Navigate chart candle at index
        actualChart.timeScale.setVisibleLogicalRange(
            from = candleIndex - 100F,
            to = candleIndex + 100F,
        )
    }

    private fun setupDefaultIndicators(
        candleSeries: CandleSeries,
        hasVolume: Boolean,
    ) {

        val closePriceIndicator = ClosePriceIndicator(candleSeries)
        val ema9Indicator = EMAIndicator(closePriceIndicator, length = 9)
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

        if (candleSeries.timeframe != Timeframe.D1) {
            sma50Plotter.setDataSource(null)
            sma100Plotter.setDataSource(null)
            sma200Plotter.setDataSource(null)
        } else {

            val sma50Indicator = SMAIndicator(closePriceIndicator, length = 50)
            val sma100Indicator = SMAIndicator(closePriceIndicator, length = 100)
            val sma200Indicator = SMAIndicator(closePriceIndicator, length = 200)

            sma50Plotter.setDataSource { index ->
                LineData(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = sma50Indicator[index].setScale(2, RoundingMode.DOWN),
                )
            }

            sma100Plotter.setDataSource { index ->
                LineData(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = sma100Indicator[index].setScale(2, RoundingMode.DOWN),
                )
            }

            sma200Plotter.setDataSource { index ->
                LineData(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = sma200Indicator[index].setScale(2, RoundingMode.DOWN),
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
        sourceCoroutineScope.cancel()
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
