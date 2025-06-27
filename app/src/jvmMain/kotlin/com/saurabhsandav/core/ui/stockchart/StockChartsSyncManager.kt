package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.ui.common.chart.toInstant
import com.saurabhsandav.lightweightcharts.data.LogicalRange
import com.saurabhsandav.lightweightcharts.data.MouseEventParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Instant

internal class StockChartsSyncManager(
    private val coroutineScope: CoroutineScope,
    private val charts: () -> Collection<StockChart>,
    private val lastActiveChartId: () -> ChartId?,
    private val syncPrefs: () -> StockChartsSyncPrefs,
) {

    fun onCandlesLoaded(stockChart: StockChart) {

        if (!syncPrefs().dateRange) return

        // If chart is not active (user hasn't interacted), skip sync
        if (lastActiveChartId() != stockChart.chartId) return

        // Update all other charts with same timeframe
        charts()
            .filter { filterStockChart ->
                // Select charts with same timeframe, ignore current chart
                stockChart.params.timeframe == filterStockChart.params.timeframe &&
                    filterStockChart != stockChart
            }
            .forEach { chart -> chart.syncLoadRangeWith(stockChart) }
    }

    fun onChartActive(stockChart: StockChart) {

        // Disable load more for non-active charts, Enable for active charts.
        charts().forEach { iStockChart ->
            iStockChart.isLoadMoreEnabled = iStockChart == stockChart
        }
    }

    fun onChartClicked(
        stockChart: StockChart,
        mouseEventParams: MouseEventParams,
    ) {

        val syncPrefs = syncPrefs()
        val charts = charts()

        if (syncPrefs.dateRange) {

            // Sync load range for all other charts with same timeframe
            charts
                .filter { filterStockChart ->
                    // Select charts with same timeframe, ignore current chart
                    stockChart.params.timeframe == filterStockChart.params.timeframe &&
                        filterStockChart != stockChart
                }
                .forEach { chart -> chart.syncLoadRangeWith(stockChart) }
        }

        if (syncPrefs.time) {

            val instant = mouseEventParams.time?.toInstant() ?: return

            // Navigate other charts to the click time value
            charts.filter { it != stockChart }.forEach { chart ->
                coroutineScope.launch {
                    chart.navigateTo(instant)
                }
            }
        }
    }

    fun onVisibleLogicalRangeChange(
        stockChart: StockChart,
        logicalRange: LogicalRange,
    ) {

        if (!syncPrefs().dateRange) return

        // If chart is not active (user hasn't interacted), skip sync
        if (lastActiveChartId() != stockChart.chartId) return

        // Current instant range. If not populated, skip sync
        val instantRange = stockChart.data.candleSeries.instantRange.value ?: return

        // Update all other charts with same timeframe
        charts()
            .filter { filterStockChart ->
                // Select charts with same timeframe, ignore current chart
                stockChart.params.timeframe == filterStockChart.params.timeframe &&
                    filterStockChart != stockChart
            }
            .forEach { chart ->

                // Chart instant range. If not populated, skip sync
                val chartInstantRange = chart.data.candleSeries.instantRange.value ?: return@forEach

                // Intersection range of current chart and iteration chart
                // Skip sync if there is no overlap in instant ranges
                val intersection = instantRange.intersect(chartInstantRange) ?: return@forEach

                // Pick a common candle instant to use for calculating a sync offset
                val commonInstant = intersection.endInclusive

                // Current chart common candle index
                val candleIndex = stockChart.data.candleSeries.binarySearch {
                    it.openInstant.compareTo(commonInstant)
                }
                // Iteration chart common candle index
                val chartCandleIndex = chart.data.candleSeries.binarySearch {
                    it.openInstant.compareTo(commonInstant)
                }

                // Sync offset for iteration chart
                val syncOffset = (chartCandleIndex - candleIndex).toFloat()

                // Set logical range with calculated offset
                chart.actualChart.timeScale.setVisibleLogicalRange(
                    from = logicalRange.from + syncOffset,
                    to = logicalRange.to + syncOffset,
                )
            }
    }

    fun onCrosshairMove(
        stockChart: StockChart,
        mouseEventParams: MouseEventParams,
    ) {

        if (!syncPrefs().crosshair) return

        // If chart is not active (user hasn't interacted), skip sync
        if (lastActiveChartId() != stockChart.chartId) return

        val charts = charts()

        if (mouseEventParams.logical == null) {
            // Crosshair doesn't exist on current chart. Clear cross-hairs on other charts
            charts.forEach { chart -> chart.actualChart.clearCrosshairPosition() }
        } else {

            // Update crosshair on all other charts with same timeframe
            charts
                .filter { filterStockChart ->
                    // Select charts with same timeframe, ignore current chart
                    stockChart.params.timeframe == filterStockChart.params.timeframe &&
                        filterStockChart != stockChart
                }
                .forEach { chart ->

                    // Set crosshair without price component
                    chart.actualChart.setCrosshairPosition(
                        price = 0.0,
                        horizontalPosition = mouseEventParams.time ?: return@forEach,
                        seriesApi = chart.plotterManager.candlestickPlotter.series,
                    )
                }
        }
    }

    private fun ClosedRange<Instant>.intersect(other: ClosedRange<Instant>): ClosedRange<Instant>? {

        // Check if the ranges intersect
        val intersects = start <= other.endInclusive && endInclusive >= other.start

        // If ranges don't intersect, return null
        if (!intersects) return null

        // Calculate the start and end of the intersection range
        val start = maxOf(start, other.start)
        val end = minOf(endInclusive, other.endInclusive)

        // Intersection range
        return start..end
    }
}
