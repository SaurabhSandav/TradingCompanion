package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.chart.plugin.TradeExecutionMarkers
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.common.chart.offsetTimeForChart
import kotlinx.datetime.Instant
import java.math.BigDecimal
import com.saurabhsandav.core.chart.plugin.TradeExecutionMarkers.Execution as ActualTradeExecutionMarker
import com.saurabhsandav.core.chart.plugin.TradeMarkers.Trade as ActualTradeMarker

class TradeExecutionMarker(
    private val instant: Instant,
    private val side: TradeExecutionSide,
    private val price: BigDecimal,
) {

    fun toActualMarker(candleSeries: CandleSeries) = ActualTradeExecutionMarker(
        time = Time.UTCTimestamp(instant.markerTime(candleSeries).offsetTimeForChart()),
        price = price,
        side = when (side) {
            TradeExecutionSide.Buy -> TradeExecutionMarkers.TradeExecutionSide.Buy
            TradeExecutionSide.Sell -> TradeExecutionMarkers.TradeExecutionSide.Sell
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
            entryPrice = entryPrice,
            exitTime = Time.UTCTimestamp(exitInstant.markerTime(candleSeries).offsetTimeForChart()),
            exitPrice = exitPrice,
            stopPrice = stopPrice,
            targetPrice = targetPrice,
        )
    }
}

private fun Instant.markerTime(candleSeries: CandleSeries): Instant {
    val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
    return candleSeries[markerCandleIndex].openInstant
}
