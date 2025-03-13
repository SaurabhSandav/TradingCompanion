package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.common.chart.offsetTimeForChart
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.indexOr
import com.saurabhsandav.lightweightcharts.data.Time
import com.saurabhsandav.lightweightcharts.plugin.TradeExecutionMarkers
import kotlinx.datetime.Instant
import java.math.BigDecimal
import com.saurabhsandav.lightweightcharts.plugin.TradeExecutionMarkers.Execution as ActualTradeExecutionMarker
import com.saurabhsandav.lightweightcharts.plugin.TradeMarkers.Trade as ActualTradeMarker

class TradeExecutionMarker(
    private val instant: Instant,
    private val side: TradeExecutionSide,
    private val price: BigDecimal,
) {

    fun toActualMarker(candleSeries: CandleSeries) = ActualTradeExecutionMarker(
        time = Time.UTCTimestamp(instant.markerTime(candleSeries).offsetTimeForChart()),
        price = price.toDouble(),
        side = when (side) {
            TradeExecutionSide.Buy -> TradeExecutionMarkers.ExecutionSide.Buy
            TradeExecutionSide.Sell -> TradeExecutionMarkers.ExecutionSide.Sell
        },
    )
}

class TradeMarker(
    private val entryInstant: Instant,
    private val entryPrice: BigDecimal,
    private val exitInstant: Instant,
    private val exitPrice: BigDecimal,
    private val stopPrice: BigDecimal,
    private val targetPrice: BigDecimal,
) {

    fun toActualMarker(candleSeries: CandleSeries): ActualTradeMarker {
        return ActualTradeMarker(
            entryTime = Time.UTCTimestamp(entryInstant.markerTime(candleSeries).offsetTimeForChart()),
            entryPrice = entryPrice.toDouble(),
            exitTime = Time.UTCTimestamp(exitInstant.markerTime(candleSeries).offsetTimeForChart()),
            exitPrice = exitPrice.toDouble(),
            stopPrice = stopPrice.toDouble(),
            targetPrice = targetPrice.toDouble(),
        )
    }
}

private fun Instant.markerTime(candleSeries: CandleSeries): Instant {

    val searchResult = candleSeries.binarySearchByAsResult(this) { it.openInstant }
    val markerCandleIndex = searchResult.indexOr { naturalIndex -> naturalIndex - 1 }

    return candleSeries[markerCandleIndex].openInstant
}
