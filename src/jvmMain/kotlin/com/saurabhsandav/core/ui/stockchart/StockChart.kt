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
import com.saurabhsandav.core.trading.indicator.SMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.trading.isLong
import com.saurabhsandav.core.ui.common.chart.*
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.plotter.*
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
    val markersAreEnabled = appModule.appPrefs.getBooleanFlow(PrefKeys.MarkersEnabled, false)

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

    fun setCandleSource(source: CandleSource): CompletableDeferred<Unit> {

        val deferred = CompletableDeferred<Unit>()

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

        sourceCoroutineScope.launch {

            fun setData() = plotters.forEach { it.setData(source.candleSeries.indices) }

            // Get the candles ready
            source.onLoad()

            // Setup Indicators
            setupDefaultIndicators(source.candleSeries, source.hasVolume)

            // Set initial data on chart
            setData()

            // Notify load complete
            deferred.complete(Unit)

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

            // Show latest 100 candles initially
            actualChart.timeScale.setVisibleLogicalRange(
                from = source.candleSeries.size - 90F,
                to = source.candleSeries.size.toFloat() + 10,
            )

            // Send an initial value through candleMarkers to start collecting the session markers flow
            val candleMarkers = source.candleMarkers.onStart { emit(emptyList()) }

            // Set markers
            markersAreEnabled.flatMapLatest { markersAreEnabled ->
                when {
                    markersAreEnabled -> generateSessionMarkers(source.candleSeries)
                    else -> flowOf(emptyList())
                }
            }
                .combine(candleMarkers) { sessionMarkers, sourceMarkers ->
                    sessionMarkers + sourceMarkers
                }
                .map { list -> list.sortedBy(SeriesMarker::instant).map(SeriesMarker::toActualMarker) }
                .onEach(candlestickPlotter::setMarkers)
                .launchIn(sourceCoroutineScope)
        }

        return deferred
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

    fun setMarkersAreEnabled(isEnabled: Boolean) = coroutineScope.launchUnit {
        appModule.appPrefs.putBoolean(PrefKeys.MarkersEnabled, isEnabled)
    }

    fun goToDateTime(dateTime: LocalDateTime?) {

        val instant = dateTime?.toInstant(TimeZone.currentSystemDefault())

        navigateToInterval(instant?.let { it..it })
    }

    fun navigateToInterval(start: Instant, end: Instant?) {
        navigateToInterval(if (end == null) start..start else start..end)
    }

    fun loadInterval(start: Instant, end: Instant? = null): CompletableDeferred<Unit> {

        val deferred = CompletableDeferred<Unit>()

        sourceCoroutineScope.launch {

            val source = checkNotNull(source) { "Source not set on chart" }

            // Load candles in range
            if (source.onLoad(start, end)) plotters.forEach { it.setData(source.candleSeries.indices) }

            // Notify load complete
            deferred.complete(Unit)
        }

        return deferred
    }

    fun destroy() {
        coroutineScope.cancel()
        sourceCoroutineScope.cancel()
        plotters.forEach { it.remove() }
        actualChart.remove()
    }

    private fun setupDefaultIndicators(
        candleSeries: CandleSeries,
        hasVolume: Boolean,
    ) {

        val closePriceIndicator = ClosePriceIndicator(candleSeries)
        val ema9Indicator = EMAIndicator(closePriceIndicator, length = 9)
        val vwapIndicator = VWAPIndicator(candleSeries, ::dailySessionStart)

        // Don't show time in daily chart
        val timeScaleOptions = TimeScaleOptions(timeVisible = candleSeries.timeframe != Timeframe.D1)
        actualChart.timeScale.applyOptions(timeScaleOptions)

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
                        candle.isLong -> Color(0, 150, 136)
                        else -> Color(255, 82, 82)
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

    private fun navigateToInterval(range: ClosedRange<Instant>?) {

        val source = checkNotNull(source) { "Source not set on chart" }

        val lastCandleIndex = source.candleSeries.lastIndex

        val candleRange = when (range) {

            // If range is not provided, go to the latest candles
            null -> lastCandleIndex..lastCandleIndex

            // Find candle indices
            else -> {

                val startCandleIndex = source.candleSeries.indexOfFirst { it.openInstant > range.start }

                // If start instant is not in current candle range, navigate to the latest candles
                if (startCandleIndex == -1) lastCandleIndex..lastCandleIndex else {

                    val endCandleIndex = source.candleSeries.indexOfFirst { it.openInstant > range.endInclusive }

                    when {
                        endCandleIndex != -1 -> startCandleIndex..endCandleIndex
                        // If end instant is not in current candle range, navigate to the start candle
                        else -> startCandleIndex..startCandleIndex
                    }
                }
            }
        }

        val diff = candleRange.last - candleRange.first
        val offset = if (diff >= 100) 10F else {
            val customOffset = (100 - diff) / 2F
            if (customOffset < 10) 10F else customOffset
        }

        // Navigate chart candle at index
        actualChart.timeScale.setVisibleLogicalRange(
            from = candleRange.first - offset,
            to = candleRange.last + offset,
        )
    }

    private fun generateSessionMarkers(candleSeries: CandleSeries): Flow<List<TradingSessionMarker>> {
        return candleSeries.instantRange.map {
            candleSeries.mapIndexedNotNull { index, candle ->
                when {
                    !dailySessionStart(candleSeries, index) -> null
                    else -> TradingSessionMarker(candle.openInstant)
                }
            }
        }
    }

    data class Params(
        val ticker: String,
        val timeframe: Timeframe,
    )
}
