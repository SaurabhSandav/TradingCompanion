package com.saurabhsandav.core.ui.stockchart.plotter

import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.chart.plugin.TradeExecutionMarkers
import com.saurabhsandav.core.trades.model.TradeExecutionSide
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

    fun toActualMarker() = ActualTradeExecutionMarker(
        time = Time.UTCTimestamp(instant.offsetTimeForChart()),
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

    fun toActualMarker(): ActualTradeMarker {
        return ActualTradeMarker(
            entryTime = Time.UTCTimestamp(entryInstant.offsetTimeForChart()),
            entryPrice = entryPrice,
            exitTime = Time.UTCTimestamp(exitInstant.offsetTimeForChart()),
            exitPrice = exitPrice,
            stopPrice = stopPrice,
            targetPrice = targetPrice,
        )
    }
}
