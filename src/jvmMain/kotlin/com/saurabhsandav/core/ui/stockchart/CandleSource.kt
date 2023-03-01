package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.stockchart.plotter.CandlestickPlotter
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class CandleSource(
    val ticker: String,
    val timeframe: Timeframe,
    val candleSeries: CandleSeries,
    val hasVolume: Boolean,
    private val onLoadBefore: (suspend () -> Boolean)? = null,
    private val onLoadAfter: (suspend () -> Boolean)? = null,
) {

    internal val coroutineScope = MainScope()
    private var moreCandlesJob: Job? = null

    internal fun init(chart: IChartApi): CandlestickPlotter {

        val candlestickPlotter = CandlestickPlotter(chart) { index ->

            val candle = candleSeries[index]

            CandlestickData(
                time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart()),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )
        }

        onLoadBefore?.let { onLoadBefore ->

            chart.timeScale.subscribeVisibleLogicalRangeChange { logicalRange ->

                if (moreCandlesJob != null || logicalRange == null) return@subscribeVisibleLogicalRangeChange

                moreCandlesJob = coroutineScope.launch {

                    val barsInfo = candlestickPlotter.series?.barsInLogicalRange(logicalRange)

                    // Load more historical data if there are less than 100 bars to the left of the visible area
                    if (barsInfo != null && barsInfo.barsBefore < 100) {
                        if (onLoadBefore()) candlestickPlotter.setData(candleSeries.indices)
                    }

                    moreCandlesJob = null
                }
            }
        }

        onLoadAfter?.let { onLoadAfter ->

            chart.timeScale.subscribeVisibleLogicalRangeChange { logicalRange ->

                if (moreCandlesJob != null || logicalRange == null) return@subscribeVisibleLogicalRangeChange

                moreCandlesJob = coroutineScope.launch {

                    val barsInfo = candlestickPlotter.series?.barsInLogicalRange(logicalRange)

                    // Load more new data if there are less than 100 bars to the right of the visible area
                    if (barsInfo != null && barsInfo.barsAfter < 100) {
                        if (onLoadAfter()) candlestickPlotter.setData(candleSeries.indices)
                    }

                    moreCandlesJob = null
                }
            }
        }

        return candlestickPlotter
    }
}
