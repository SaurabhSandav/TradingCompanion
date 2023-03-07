package com.saurabhsandav.core.ui.stockchart

import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.common.chart.visibleLogicalRangeChange
import com.saurabhsandav.core.ui.stockchart.plotter.CandlestickPlotter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CandleSource(
    val ticker: String,
    val timeframe: Timeframe,
    val hasVolume: Boolean,
    private val onLoad: suspend () -> CandleSeries,
    private val onLoadBefore: (suspend () -> Boolean)? = null,
    private val onLoadAfter: (suspend () -> Boolean)? = null,
) {

    internal val coroutineScope = MainScope()
    lateinit var candleSeries: CandleSeries

    internal suspend fun init(
        chart: IChartApi,
        candlestickPlotter: CandlestickPlotter,
        onResetData: () -> Unit,
    ) {

        candleSeries = onLoad()

        if (onLoadBefore != null || onLoadAfter != null) {

            chart.timeScale
                .visibleLogicalRangeChange()
                .conflate()
                .filterNotNull()
                .onEach { logicalRange ->

                    val barsInfo = candlestickPlotter.series?.barsInLogicalRange(logicalRange) ?: return@onEach

                    when {
                        // Load more historical data if there are less than 100 bars to the left of the visible area
                        barsInfo.barsBefore < 100 && onLoadBefore?.invoke() == true -> onResetData()

                        // Load more new data if there are less than 100 bars to the right of the visible area
                        barsInfo.barsAfter < 100 && onLoadAfter?.invoke() == true -> onResetData()
                    }
                }
                .launchIn(coroutineScope)
        }
    }
}
