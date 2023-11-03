package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.russhwolf.settings.coroutines.FlowSettings
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
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.newChildScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.math.RoundingMode
import kotlin.time.Duration.Companion.milliseconds

@Stable
class StockChart(
    parentScope: CoroutineScope,
    private val prefs: FlowSettings,
    private val marketDataProvider: MarketDataProvider,
    private val candleLoader: CandleLoader,
    val actualChart: IChartApi,
    initialData: StockChartData,
    initialVisibleRange: ClosedRange<Float>? = null,
    onLegendUpdate: (List<String>) -> Unit,
) {

    private val coroutineScope = parentScope.newChildScope()
    private var dataCoroutineScope = coroutineScope.newChildScope()

    private val candlestickPlotter = CandlestickPlotter("candles")
    private val volumePlotter = VolumePlotter("volume")
    private val vwapPlotter = LinePlotter("vwap", "VWAP", Color(0xFFA500))
    private val ema9Plotter = LinePlotter("ema9", "EMA (9)")
    private val sma50Plotter = LinePlotter("sma50", "SMA (50)", Color(0x0AB210))
    private val sma100Plotter = LinePlotter("sma100", "SMA (100)", Color(0xB05F10))
    private val sma200Plotter = LinePlotter("sma200", "SMA (200)", Color(0xB00C10))

    var visibleRange: ClosedRange<Float>? = initialVisibleRange

    var data: StockChartData = initialData
        private set
    var params by mutableStateOf(initialData.params)
    val title by derivedStateOf { "${params.ticker} (${params.timeframe.toLabel()})" }
    val plotters = mutableStateListOf<SeriesPlotter<*>>()
    val markersAreEnabled = prefs.getBooleanFlow(PrefMarkersEnabled, false)

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

        plotters.forEach { plotter -> plotter.onAttach(this) }

        actualChart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true)
        )

        // Legend updates
        actualChart.crosshairMove().onEach { params ->
            onLegendUpdate(plotters.map { it.legendText(params) })
        }.launchIn(coroutineScope)

        // Observe plotter enabled prefs
        plotters.forEach { plotter ->
            prefs
                .getBooleanFlow(plotter.prefKey, true)
                .onEach(plotter::setIsEnabled)
                .launchIn(coroutineScope)
        }

        // Set initial StockChartData
        setData(initialData)
    }

    fun setData(data: StockChartData) {

        val prevParams = params

        // Update chart params
        params = data.params

        // Update legend title for candles
        candlestickPlotter.legendLabel = title

        // Cancel CoroutineScope for the previous StockChartData
        dataCoroutineScope.cancel()

        // Update data
        this@StockChart.data = data
        dataCoroutineScope = coroutineScope.newChildScope()

        dataCoroutineScope.launch {

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
            candleSeries.modifications.onEach { refresh() }.launchIn(dataCoroutineScope)

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

                    val barsInfo = candlestickPlotter.series.barsInLogicalRange(logicalRange) ?: return@onEach

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
                .launchIn(dataCoroutineScope)

            // Update chart with live candles
            candleSeries
                .live
                .onEach { candle ->
                    plotters.forEach {
                        it.update(candleSeries.indexOf(candle))
                    }
                }
                .launchIn(dataCoroutineScope)

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
        prefs.putBoolean(plotter.prefKey, isEnabled)
    }

    fun setMarkersAreEnabled(isEnabled: Boolean) = coroutineScope.launchUnit {
        prefs.putBoolean(PrefMarkersEnabled, isEnabled)
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
        dataCoroutineScope.cancel()
        actualChart.remove()
        plotters.clear()
    }

    private fun setupCandlesAndIndicators(
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
            .launchIn(dataCoroutineScope)
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

    private val SeriesPlotter<*>.prefKey
        get() = "plotter_${key}_enabled"
}

private const val PrefMarkersEnabled = "markers_enabled"

internal const val StockChartLoadMoreThreshold = 50
internal const val StockChartLoadInstantBuffer = 100
