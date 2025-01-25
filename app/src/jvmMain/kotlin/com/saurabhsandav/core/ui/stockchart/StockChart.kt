package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trading.*
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import com.saurabhsandav.core.trading.indicator.SMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.ui.common.chart.*
import com.saurabhsandav.core.ui.common.hex
import com.saurabhsandav.core.ui.common.toCssColor
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.StockChartData.LoadState
import com.saurabhsandav.core.ui.stockchart.plotter.CandlestickPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.LinePlotter
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.VolumePlotter
import com.saurabhsandav.core.utils.*
import com.saurabhsandav.core.utils.BinarySearchResult.NotFound
import com.saurabhsandav.lightweight_charts.IChartApi
import com.saurabhsandav.lightweight_charts.data.CandlestickData
import com.saurabhsandav.lightweight_charts.data.HistogramData
import com.saurabhsandav.lightweight_charts.data.LineData
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.options.TimeScaleOptions
import com.saurabhsandav.lightweight_charts.plugin.SessionMarkers
import com.saurabhsandav.lightweight_charts.plugin.TradeExecutionMarkers
import com.saurabhsandav.lightweight_charts.plugin.TradeMarkers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.math.RoundingMode
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class StockChart(
    parentScope: CoroutineScope,
    private val prefs: FlowSettings,
    private val marketDataProvider: MarketDataProvider,
    private val candleLoader: CandleLoader,
    val actualChart: IChartApi,
    initialData: StockChartData,
    initialVisibleRange: ClosedRange<Float>? = null,
) {

    private val coroutineScope = parentScope.newChildScope()
    private var dataCoroutineScope = coroutineScope.newChildScope()

    internal val candlestickPlotter = CandlestickPlotter("candles")
    private val volumePlotter = VolumePlotter("volume")
    private val vwapPlotter = LinePlotter("vwap", "VWAP", Color.hex("#FFA500"))
    private val ema9Plotter = LinePlotter("ema9", "EMA (9)", Color.hex("#2962FF"))
    private val ema21Plotter = LinePlotter("ema21", "EMA (21)", Color.hex("#F7525F"))
    private val sma50Plotter = LinePlotter("sma50", "SMA (50)", Color.hex("#0AB210"))
    private val sma100Plotter = LinePlotter("sma100", "SMA (100)", Color.hex("#B05F10"))
    private val sma200Plotter = LinePlotter("sma200", "SMA (200)", Color.hex("#B00C10"))
    private val sessionMarkers = SessionMarkers()
    private val tradeExecutionMarkers = TradeExecutionMarkers()
    private val tradeMarkers = TradeMarkers()

    private var indicators: Indicators? = null

    var visibleRange: ClosedRange<Float>? = initialVisibleRange

    var data: StockChartData = initialData
        private set
    var params by mutableStateOf(initialData.params)
    val title by derivedStateOf { "${params.ticker} (${params.timeframe.toLabel()})" }
    val plotters = mutableStateListOf<Plotter<*>>()
    val markersAreEnabled = prefs.getBooleanFlow(PrefMarkersEnabled, false)

    private var initialized = CompletableDeferred<Unit>()

    init {

        plotters.addAll(
            listOf(
                candlestickPlotter,
                volumePlotter,
                vwapPlotter,
                ema9Plotter,
                ema21Plotter,
                sma50Plotter,
                sma100Plotter,
                sma200Plotter,
            )
        )

        plotters.forEach { plotter -> plotter.onAttach(this) }

        candlestickPlotter.series.attachPrimitive(sessionMarkers)
        candlestickPlotter.series.attachPrimitive(tradeExecutionMarkers)
        candlestickPlotter.series.attachPrimitive(tradeMarkers)

        actualChart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true)
        )

        // Observe plotter enabled prefs
        plotters.forEach { plotter ->
            prefs
                .getBooleanFlow(plotter.prefKey, true)
                .onEach { plotter.isEnabled = it }
                .launchIn(coroutineScope)
        }

        // Set initial StockChartData
        setData(initialData)

        // Legend updates
        actualChart
            .crosshairMove()
            .onEach { params ->

                plotters.forEach { plotter ->
                    if (plotter is SeriesPlotter<*, *>) plotter.updateLegendValues(params)
                }
            }
            .launchIn(coroutineScope)
    }

    fun setData(data: StockChartData) {

        if (!initialized.isCompleted) initialized.cancel()
        initialized = CompletableDeferred()

        val prevParams = params

        // Update chart params
        params = data.params

        // Update legend title for candles
        candlestickPlotter.legendLabel = AnnotatedString(title)

        // Cancel CoroutineScope for the previous StockChartData
        dataCoroutineScope.cancel()

        // Update data
        this@StockChart.data = data
        dataCoroutineScope = coroutineScope.newChildScope()

        dataCoroutineScope.launch {

            // Await first load
            data.candleSeries.modifications.first()

            val candleSeries = data.candleSeries

            // Don't show time in daily chart
            val timeScaleOptions = TimeScaleOptions(timeVisible = candleSeries.timeframe != Timeframe.D1)
            actualChart.timeScale.applyOptions(timeScaleOptions)

            // Setup indicators
            indicators = Indicators(
                candleSeries = candleSeries,
                params = params,
                hasVolume = marketDataProvider.hasVolume(params),
                sessionChecker = marketDataProvider.sessionChecker(),
            )

            // If no initialVisibleRange provided, show latest 90 candles (with a 10 candle empty area).
            // On ticker change, restore visible range.
            val finalVisibleRange = when {
                prevParams.timeframe != params.timeframe -> null
                else -> this@StockChart.visibleRange
            } ?: (candleSeries.size - 90F)..(candleSeries.size + 10F)

            // Set initial data
            setDataToChart()

            // Set visible range
            actualChart.timeScale.setVisibleLogicalRange(
                from = finalVisibleRange.start,
                to = finalVisibleRange.endInclusive,
            )

            // Load before/after candles if needed
            actualChart.timeScale
                .visibleLogicalRangeChange()
                .debounce(300.milliseconds)
                .filterNotNull()
                .onEach { logicalRange ->

                    // Save visible range
                    this@StockChart.visibleRange = logicalRange.from..logicalRange.to

                    // If a load is ongoing don't load before/after
                    if (data.loadState.first() == LoadState.Loading) return@onEach

                    val barsInfo = candlestickPlotter.series.barsInLogicalRange(logicalRange) ?: return@onEach

                    when {
                        // Load more historical data if there are less than a certain no. of bars to the left of the visible area.
                        barsInfo.barsBefore < candleLoader.loadConfig.loadMoreThreshold -> {

                            // Load
                            candleLoader.loadBefore(
                                params = params,
                                loadCount = barsInfo.barsBefore.takeIf { it < 0 }?.absoluteValue?.roundToInt(),
                            )
                        }

                        // Load more new data if there are less than a certain no. of bars to the right of the visible area.
                        barsInfo.barsAfter < candleLoader.loadConfig.loadMoreThreshold -> {

                            // Load
                            candleLoader.loadAfter(
                                params = params,
                                loadCount = barsInfo.barsAfter.takeIf { it < 0 }?.absoluteValue?.roundToInt(),
                            )
                        }
                    }
                }
                .launchIn(dataCoroutineScope)

            // Update chart with live candles
            candleSeries.live.onEach { (i, candle) -> update(i, candle) }.launchIn(dataCoroutineScope)

            setupMarkers()

            // Set latest values to legend
            plotters.forEach { plotter ->
                if (plotter is SeriesPlotter<*, *>) plotter.updateLegendValues(null)
            }

            // Signal initialization completion
            initialized.complete(Unit)
        }
    }

    fun setDarkMode(isDark: Boolean) {
        actualChart.applyOptions(if (isDark) ChartDarkModeOptions else ChartLightModeOptions)
    }

    fun setPlotterIsEnabled(plotter: Plotter<*>, isEnabled: Boolean) = coroutineScope.launchUnit {
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

    internal fun setDataToChart() {

        val indicators = indicators ?: return
        val candleSeries = indicators.candleSeries

        candlestickPlotter.setData(candleSeries.map { candle ->

            CandlestickData.Item(
                time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart()),
                open = candle.open.toDouble(),
                high = candle.high.toDouble(),
                low = candle.low.toDouble(),
                close = candle.close.toDouble(),
            )
        })

        ema9Plotter.setData(candleSeries.indices.map { index ->
            LineData.Item(
                time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                value = indicators.ema9Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
            )
        })

        ema21Plotter.setData(candleSeries.indices.map { index ->
            LineData.Item(
                time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                value = indicators.ema21Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
            )
        })

        if (indicators.hasVolume) {

            volumePlotter.setData(candleSeries.indices.map { index ->

                val candle = candleSeries[index]

                HistogramData.Item(
                    time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart()),
                    value = candle.volume.toDouble(),
                    color = Color.hex(if (candle.isLong) "#009688" else "#FF5252")?.toCssColor(),
                )
            })
        } else volumePlotter.setData(emptyList())

        val vwapIndicator = indicators.vwapIndicator

        if (vwapIndicator != null) {

            vwapPlotter.setData(candleSeries.indices.map { index ->
                LineData.Item(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = vwapIndicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
                )
            })
        } else vwapPlotter.setData(emptyList())

        val sma50Indicator = indicators.sma50Indicator

        if (sma50Indicator != null) {

            sma50Plotter.setData(candleSeries.indices.map { index ->
                LineData.Item(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = sma50Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
                )
            })
        } else sma50Plotter.setData(emptyList())

        val sma100Indicator = indicators.sma100Indicator

        if (sma100Indicator != null) {

            sma100Plotter.setData(candleSeries.indices.map { index ->
                LineData.Item(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = sma100Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
                )
            })
        } else sma100Plotter.setData(emptyList())

        val sma200Indicator = indicators.sma200Indicator

        if (sma200Indicator != null) {

            sma200Plotter.setData(candleSeries.indices.map { index ->
                LineData.Item(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = sma200Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
                )
            })
        } else sma200Plotter.setData(emptyList())
    }

    private fun update(
        index: Int,
        candle: Candle,
    ) {

        val indicators = requireNotNull(indicators)
        val time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart())

        candlestickPlotter.update(
            CandlestickData.Item(
                time = time,
                open = candle.open.toDouble(),
                high = candle.high.toDouble(),
                low = candle.low.toDouble(),
                close = candle.close.toDouble(),
            )
        )

        ema9Plotter.update(
            LineData.Item(
                time = time,
                value = indicators.ema9Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
            )
        )

        ema21Plotter.update(
            LineData.Item(
                time = time,
                value = indicators.ema21Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
            )
        )

        if (indicators.hasVolume) {

            volumePlotter.update(
                HistogramData.Item(
                    time = time,
                    value = candle.volume.toDouble(),
                    color = Color.hex(if (candle.isLong) "#009688" else "#FF5252")?.toCssColor(),
                )
            )
        }

        val vwapIndicator = indicators.vwapIndicator

        if (vwapIndicator != null) {

            vwapPlotter.update(
                LineData.Item(
                    time = time,
                    value = vwapIndicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
                )
            )
        }

        val sma50Indicator = indicators.sma50Indicator

        if (sma50Indicator != null) {

            sma50Plotter.update(
                LineData.Item(
                    time = time,
                    value = sma50Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
                )
            )
        }

        val sma100Indicator = indicators.sma100Indicator

        if (sma100Indicator != null) {

            sma100Plotter.update(
                LineData.Item(
                    time = time,
                    value = sma100Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
                )
            )
        }

        val sma200Indicator = indicators.sma200Indicator

        if (sma200Indicator != null) {

            sma200Plotter.update(
                LineData.Item(
                    time = time,
                    value = sma200Indicator[index].setScale(2, RoundingMode.DOWN).toDouble(),
                )
            )
        }
    }

    private fun setupMarkers() {

        val candleSeries = data.candleSeries

        // Session markers
        generateSessionStartInstants(candleSeries)
            .onEach { instants ->
                val times = instants.map { Time.UTCTimestamp(it.offsetTimeForChart()) }
                sessionMarkers.setTimes(times)
            }
            .launchIn(dataCoroutineScope)

        // Trade execution markers
        combine(markersAreEnabled, data.tradeExecutionMarkers) { markersEnabled, tradeExecutionMarkers ->
            if (markersEnabled) tradeExecutionMarkers else emptyList()
        }.mapList { it.toActualMarker(candleSeries) }
            .onEach(tradeExecutionMarkers::setExecutions)
            .launchIn(dataCoroutineScope)

        // Trade execution markers
        combine(markersAreEnabled, data.tradeMarkers) { markersEnabled, tradeMarkers ->
            if (markersEnabled) tradeMarkers else emptyList()
        }.mapList { it.toActualMarker(candleSeries) }
            .onEach(tradeMarkers::setTrades)
            .launchIn(dataCoroutineScope)
    }

    private suspend fun navigateToInterval(interval: ClosedRange<Instant>?) {

        val candleSeries = data.candleSeries

        // Wait for any loading to finish
        initialized.await()

        // No candles loaded, nothing to do
        if (candleSeries.isEmpty()) return

        // Temporarily disable anchoring visible range to latest candles
        actualChart.timeScale.applyOptions(TimeScaleOptions(shiftVisibleRangeOnNewBar = false))

        val candleRange = when (interval) {

            // If range is not provided, go to the latest candles
            null -> null

            // Find candle indices
            else -> run {

                // Load data for specified interval
                candleLoader.load(
                    params = params,
                    interval = interval,
                )

                val startIndexResult = candleSeries.binarySearchByAsResult(interval.start) { it.openInstant }

                // If start instant is not in current candle range, navigate to the latest candles
                if (startIndexResult is NotFound && startIndexResult.isOutsideRange) return@run null

                val startIndex = startIndexResult.indexOr { naturalIndex -> naturalIndex - 1 }
                val endIndexResult = candleSeries.binarySearchByAsResult(interval.endInclusive) { it.openInstant }

                // If end instant is not in current candle range, navigate to the start candle
                if (endIndexResult is NotFound && endIndexResult.isOutsideRange) {
                    return@run startIndex..startIndex
                }

                return@run startIndex..endIndexResult.indexOrNaturalIndex
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
            else -> {

                // Load latest data
                candleLoader.loadLatest(params)

                (candleSeries.size - 90F) to (candleSeries.size + 10F)
            }
        }

        actualChart.timeScale.setVisibleLogicalRange(from = from, to = to)
        actualChart.timeScale.applyOptions(TimeScaleOptions(shiftVisibleRangeOnNewBar = true))
    }

    private fun generateSessionStartInstants(candleSeries: CandleSeries): Flow<List<Instant>> {
        return candleSeries.instantRange.map {
            candleSeries
                .filterIndexed { index, _ -> marketDataProvider.sessionChecker().isSessionStart(candleSeries, index) }
                .map { it.openInstant }
        }
    }

    private val Plotter<*>.prefKey
        get() = "plotter_${key}_enabled"

    private class Indicators(
        val candleSeries: CandleSeries,
        val params: StockChartParams,
        val hasVolume: Boolean,
        sessionChecker: SessionChecker,
    ) {

        private val isDaily = params.timeframe == Timeframe.D1

        val closePriceIndicator = ClosePriceIndicator(candleSeries)
        val ema9Indicator = EMAIndicator(closePriceIndicator, length = 9)
        val ema21Indicator = EMAIndicator(closePriceIndicator, length = 21)
        val vwapIndicator = VWAPIndicator(candleSeries, sessionChecker).takeIf { hasVolume }

        val sma50Indicator = SMAIndicator(closePriceIndicator, length = 50).takeIf { isDaily }
        val sma100Indicator = SMAIndicator(closePriceIndicator, length = 100).takeIf { isDaily }
        val sma200Indicator = SMAIndicator(closePriceIndicator, length = 200).takeIf { isDaily }
    }
}

private const val PrefMarkersEnabled = "markers_enabled"
