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
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import com.saurabhsandav.core.trading.indicator.SMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.trading.isLong
import com.saurabhsandav.core.ui.common.chart.*
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.StockChartData.LoadState
import com.saurabhsandav.core.ui.stockchart.plotter.*
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.math.RoundingMode
import kotlin.time.Duration.Companion.milliseconds

@Stable
internal class StockChart(
    val appModule: AppModule,
    private val marketDataProvider: MarketDataProvider,
    private val candleLoader: CandleLoader,
    val actualChart: IChartApi,
    initialData: StockChartData,
    initialVisibleRange: ClosedRange<Float>? = null,
    onLegendUpdate: (List<String>) -> Unit,
) {

    private var sourceCoroutineScope = MainScope()

    private val candlestickPlotter = CandlestickPlotter(actualChart)
    private val volumePlotter = VolumePlotter(actualChart)
    private val vwapPlotter = LinePlotter(actualChart, "VWAP", Color(0xFFA500))
    private val ema9Plotter = LinePlotter(actualChart, "EMA (9)")
    private val sma50Plotter = LinePlotter(actualChart, "SMA (50)", Color(0x0AB210))
    private val sma100Plotter = LinePlotter(actualChart, "SMA (100)", Color(0xB05F10))
    private val sma200Plotter = LinePlotter(actualChart, "SMA (200)", Color(0xB00C10))

    var visibleRange: ClosedRange<Float>? = initialVisibleRange

    val coroutineScope = MainScope()
    var data: StockChartData = initialData
        private set
    var params by mutableStateOf(initialData.params)
    val title by derivedStateOf { "${params.ticker} (${params.timeframe.toLabel()})" }
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

        // Observe plotter enabled prefs
        observerPlotterIsEnabled(PrefKeys.PlotterCandlesEnabled, candlestickPlotter)
        observerPlotterIsEnabled(PrefKeys.PlotterVolumeEnabled, volumePlotter)
        observerPlotterIsEnabled(PrefKeys.PlotterVWAPEnabled, vwapPlotter)
        observerPlotterIsEnabled(PrefKeys.PlotterEMA9Enabled, ema9Plotter)
        observerPlotterIsEnabled(PrefKeys.PlotterSMA50Enabled, sma50Plotter)
        observerPlotterIsEnabled(PrefKeys.PlotterSMA100Enabled, sma100Plotter)
        observerPlotterIsEnabled(PrefKeys.PlotterSMA200Enabled, sma200Plotter)

        // Set initial StockChartData
        setData(initialData)
    }

    fun setData(data: StockChartData) {

        val prevParams = params

        // Update chart params
        params = data.params

        // Update legend title for candles
        candlestickPlotter.name = title

        // Cancel CoroutineScope for the previous CandleSource
        sourceCoroutineScope.cancel()

        // Update data
        this@StockChart.data = data
        sourceCoroutineScope = MainScope()

        sourceCoroutineScope.launch {

            // Wait for first load
            data.loadState.first { loadState -> loadState == LoadState.Loaded }

            val candleSeries = data.getCandleSeries()

            // Setup chart
            setupCandlesAndIndicators(
                candleSeries = candleSeries,
                hasVolume = marketDataProvider.hasVolume(params),
            )

            // If no initialVisibleRange provided, show latest 90 candles (with a 10 candle empty area).
            // On ticker change, restore visible range.
            val finalVisibleRange = when {
                prevParams.timeframe != params.timeframe -> null
                else -> this@StockChart.visibleRange
            } ?: (candleSeries.size - 90F)..(candleSeries.size + 10F)

            // Set initial data
            refresh()

            // Set visible range
            actualChart.timeScale.setVisibleLogicalRange(
                from = finalVisibleRange.start,
                to = finalVisibleRange.endInclusive,
            )

            // Set data on future loads
            candleSeries.modifications.onEach { refresh() }.launchIn(sourceCoroutineScope)

            // Load before/after candles if needed
            actualChart.timeScale
                .visibleLogicalRangeChange()
                .conflate()
                .filterNotNull()
                .onEach { logicalRange ->

                    // Save visible range
                    this@StockChart.visibleRange = logicalRange.from..logicalRange.to

                    // If a load is ongoing don't load before/after
                    if (data.loadState.first() == LoadState.Loading) return@onEach

                    val barsInfo = candlestickPlotter.series?.barsInLogicalRange(logicalRange) ?: return@onEach

                    when {
                        // Load more historical data if there are less than a certain no. of bars to the left of the visible area.
                        barsInfo.barsBefore < StockChartLoadMoreThreshold -> {

                            // Load
                            candleLoader.loadBefore(params)

                            // Wait for loaded candles to be set to chart. Prevents un-necessary loads.
                            delay(500.milliseconds)
                        }

                        // Load more new data if there are less than a certain no. of bars to the right of the visible area.
                        barsInfo.barsAfter < StockChartLoadMoreThreshold -> {

                            // Load
                            candleLoader.loadAfter(params)

                            // Wait for loaded candles to be set to chart. Prevents un-necessary loads.
                            delay(500.milliseconds)
                        }
                    }
                }
                .launchIn(sourceCoroutineScope)

            // Update chart with live candles
            candleSeries
                .live
                .onEach { candle ->
                    plotters.forEach {
                        it.update(candleSeries.indexOf(candle))
                    }
                }
                .launchIn(sourceCoroutineScope)

            setupMarkers()
        }
    }

    suspend fun refresh() {
        plotters.forEach { it.setData(data.getCandleSeries().indices) }
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

    suspend fun navigateTo(
        instant: Instant? = null,
        to: Instant? = null,
    ) {

        val range = when {
            instant == null -> null
            to == null -> instant..instant
            else -> instant..to
        }

        navigateToInterval(range)
    }

    fun destroy() {
        coroutineScope.cancel()
        sourceCoroutineScope.cancel()
        plotters.forEach { it.remove() }
        actualChart.remove()
    }

    private suspend fun setupCandlesAndIndicators(
        candleSeries: CandleSeries,
        hasVolume: Boolean,
    ) {

        val closePriceIndicator = ClosePriceIndicator(candleSeries)
        val ema9Indicator = EMAIndicator(closePriceIndicator, length = 9)
        val vwapIndicator = VWAPIndicator(candleSeries, marketDataProvider.sessionChecker())

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

    private suspend fun setupMarkers() {

        val candleSeries = data.getCandleSeries()
        val candleMarkers = candleSeries.instantRange.flatMapLatest { data.getCandleMarkers() }

        // Set markers
        markersAreEnabled
            .flatMapLatest { markersAreEnabled ->
                when {
                    markersAreEnabled -> {

                        val flows = listOf(
                            // Always emits a list
                            generateSessionMarkers(candleSeries),
                            // May not always emit something. Emit an empty list to start combine
                            candleMarkers.onStart { emit(emptyList()) },
                        )

                        combine(flows) { it.toList().flatten() }
                    }

                    else -> flowOf(emptyList())
                }
            }
            .map { list -> list.sortedBy(SeriesMarker::instant).map(SeriesMarker::toActualMarker) }
            .onEach(candlestickPlotter::setMarkers)
            .launchIn(sourceCoroutineScope)
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

    private suspend fun navigateToInterval(interval: ClosedRange<Instant>?) {

        // Wait for any loading to finish
        data.loadState.first { loadState -> loadState == LoadState.Loaded }

        val candleSeries = data.getCandleSeries()

        // No candles loaded, nothing to do
        if (candleSeries.isEmpty()) return

        val candleRange = when (interval) {

            // If range is not provided, go to the latest candles
            null -> null

            // Find candle indices
            else -> {

                // Load data for specified interval
                candleLoader.load(
                    params = params,
                    instant = interval.start,
                    to = interval.endInclusive,
                )

                val startCandleIndex = candleSeries.indexOfFirst { it.openInstant > interval.start }

                // If start instant is not in current candle range, navigate to the latest candles
                if (startCandleIndex == -1) null else {

                    val endCandleIndex = candleSeries.indexOfFirst { it.openInstant > interval.endInclusive }

                    when {
                        endCandleIndex != -1 -> startCandleIndex..endCandleIndex
                        // If end instant is not in current candle range, navigate to the start candle
                        else -> startCandleIndex..startCandleIndex
                    }
                }
            }
        }

        val (from, to) = when {
            candleRange != null -> {

                // Add a 10 candle buffer on either sides if interval is greater than 100 candles.
                // Else, add enough buffer candles to fit 100 candles on chart (minimum 10 candle buffer)
                val diff = candleRange.last - candleRange.first
                val offset = if (diff >= 100) 10F else {
                    val customOffset = (100 - diff) / 2F
                    if (customOffset < 10) 10F else customOffset
                }

                (candleRange.first - offset) to (candleRange.last + offset)
            }

            // Show latest 90 candles (with a 10 candle empty area)
            else -> (candleSeries.size - 90F) to (candleSeries.size + 10F)
        }

        actualChart.timeScale.setVisibleLogicalRange(from = from, to = to)
    }

    private fun generateSessionMarkers(candleSeries: CandleSeries): Flow<List<TradingSessionMarker>> {
        return candleSeries.instantRange.map {
            candleSeries.mapIndexedNotNull { index, candle ->
                when {
                    !marketDataProvider.sessionChecker().isSessionStart(candleSeries, index) -> null
                    else -> TradingSessionMarker(candle.openInstant)
                }
            }
        }
    }
}

internal const val StockChartLoadMoreThreshold = 50
internal const val StockChartLoadInstantBuffer = 100
