package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.data.SeriesMarkerPosition
import com.saurabhsandav.core.chart.data.SeriesMarkerShape
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.ui.common.chart.offsetTimeForChart
import kotlinx.datetime.Instant
import java.math.BigDecimal
import com.saurabhsandav.core.chart.data.SeriesMarker as ActualSeriesMarker

interface SeriesMarker {

    val instant: Instant

    fun toActualMarker(): ActualSeriesMarker
}

class TradingSessionMarker(
    override val instant: Instant,
) : SeriesMarker {

    override fun toActualMarker() = ActualSeriesMarker(
        time = Time.UTCTimestamp(instant.offsetTimeForChart()),
        position = SeriesMarkerPosition.AboveBar,
        shape = SeriesMarkerShape.Square,
        color = Color(0x6C00FF),
    )
}

class TradeOrderMarker(
    override val instant: Instant,
    private val side: TradeExecutionSide,
    private val price: BigDecimal,
) : SeriesMarker {

    override fun toActualMarker() = ActualSeriesMarker(
        time = Time.UTCTimestamp(instant.offsetTimeForChart()),
        position = when (side) {
            TradeExecutionSide.Buy -> SeriesMarkerPosition.BelowBar
            TradeExecutionSide.Sell -> SeriesMarkerPosition.AboveBar
        },
        shape = when (side) {
            TradeExecutionSide.Buy -> SeriesMarkerShape.ArrowUp
            TradeExecutionSide.Sell -> SeriesMarkerShape.ArrowDown
        },
        color = when (side) {
            TradeExecutionSide.Buy -> Color.Green
            TradeExecutionSide.Sell -> Color.Red
        },
        text = when (side) {
            TradeExecutionSide.Buy -> price.toPlainString()
            TradeExecutionSide.Sell -> price.toPlainString()
        },
    )
}

class TradeMarker(
    override val instant: Instant,
    private val isEntry: Boolean,
) : SeriesMarker {

    override fun toActualMarker() = ActualSeriesMarker(
        time = Time.UTCTimestamp(instant.offsetTimeForChart()),
        position = SeriesMarkerPosition.AboveBar,
        shape = SeriesMarkerShape.Circle,
        color = if (isEntry) Color.Green else Color.Red,
    )
}
