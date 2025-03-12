package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.ChartDarkModeOptions
import com.saurabhsandav.core.ui.common.chart.ChartLightModeOptions
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.common.chart.visibleLogicalRangeChange
import com.saurabhsandav.core.ui.common.toLabel
import com.saurabhsandav.core.ui.stockchart.data.CandleLoader
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.data.StockChartData
import com.saurabhsandav.core.ui.stockchart.data.StockChartData.LoadState
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesPlotter
import com.saurabhsandav.core.utils.BinarySearchResult.NotFound
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.indexOr
import com.saurabhsandav.core.utils.indexOrNaturalIndex
import com.saurabhsandav.core.utils.newChildScope
import com.saurabhsandav.lightweight_charts.IChartApi
import com.saurabhsandav.lightweight_charts.options.TimeScaleOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class StockChart internal constructor(
    parentScope: CoroutineScope,
    val chartId: ChartId,
    prefs: FlowSettings,
    private val marketDataProvider: MarketDataProvider,
    private val candleLoader: CandleLoader,
    val actualChart: IChartApi,
    initialData: StockChartData,
    syncManager: StockChartsSyncManager,
    private val onShowTickerSelector: () -> Unit,
    private val onShowTimeframeSelector: () -> Unit,
    initialVisibleRange: ClosedRange<Float>? = null,
) {

    private val coroutineScope = parentScope.newChildScope()
    private var dataCoroutineScope = coroutineScope.newChildScope()

    internal var data: StockChartData = initialData
        private set

    var visibleRange: ClosedRange<Float>? = initialVisibleRange
    var params by mutableStateOf(initialData.params)
    val title by derivedStateOf { "${params.ticker} (${params.timeframe.toLabel()})" }
    val plotterManager = PlotterManager(coroutineScope, this, prefs)

    private var initialized = CompletableDeferred<Unit>()

    init {

        actualChart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true),
        )

        // Set initial StockChartData
        setData(initialData)

        // Legend updates
        actualChart
            .crosshairMove()
            .onEach { params ->

                plotterManager.plotters.forEach { plotter ->
                    if (plotter is SeriesPlotter<*, *>) plotter.updateLegendValues(params)
                }
            }
            .launchIn(coroutineScope)

        // Subscribe visible logical range
        actualChart.timeScale
            .visibleLogicalRangeChange()
            .filterNotNull()
            .onEach { logicalRange ->
                syncManager.onVisibleLogicalRangeChange(this, logicalRange)
            }
            .launchIn(coroutineScope)

        // Subscribe crosshair move
        actualChart
            .crosshairMove()
            .onEach { mouseEventParams ->
                syncManager.onCrosshairMove(this, mouseEventParams)
            }
            .launchIn(coroutineScope)
    }

    internal fun setData(data: StockChartData) {

        if (!initialized.isCompleted) initialized.cancel()
        initialized = CompletableDeferred()

        val prevParams = params

        // Update chart params
        params = data.params

        // Update legend title for candles
        plotterManager.candlestickPlotter.legendLabel = buildAnnotatedString {
            withLink(LinkAnnotation.Clickable("ticker") { onShowTickerSelector() }) {
                append(params.ticker)
            }
            append(" • ")
            withLink(LinkAnnotation.Clickable("timeframe") { onShowTimeframeSelector() }) {
                append(params.timeframe.toLabel())
            }
        }

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
            plotterManager.onSetStockChartData(
                data = data,
                hasVolume = marketDataProvider.hasVolume(params),
                sessionChecker = marketDataProvider.sessionChecker(),
                dataCoroutineScope = dataCoroutineScope,
            )

            // If no initialVisibleRange provided, show latest 90 candles (with a 10 candle empty area).
            // On ticker change, restore visible range.
            val finalVisibleRange = when {
                prevParams.timeframe != params.timeframe -> null
                else -> this@StockChart.visibleRange
            } ?: (candleSeries.size - 90F)..(candleSeries.size + 10F)

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

                    val barsInfo = plotterManager.candlestickPlotter.series
                        .barsInLogicalRange(logicalRange)
                        ?: return@onEach

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
            candleSeries.live
                .onEach { (i, candle) -> plotterManager.update(i, candle) }
                .launchIn(dataCoroutineScope)

            // Signal initialization completion
            initialized.complete(Unit)
        }
    }

    fun setDarkMode(isDark: Boolean) {
        actualChart.applyOptions(if (isDark) ChartDarkModeOptions else ChartLightModeOptions)
    }

    suspend fun navigateToLatest() = navigateTo(null)

    suspend fun navigateTo(
        instant: Instant?,
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
                val offset = if (diff >= 100) {
                    10F
                } else {
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
}
