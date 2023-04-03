package com.saurabhsandav.core.ui.stockchart.plotter

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.data.SeriesMarkerPosition
import com.saurabhsandav.core.chart.data.SeriesMarkerShape
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.ui.common.chart.offsetTimeForChart
import kotlinx.datetime.Instant
import java.math.BigDecimal
import com.saurabhsandav.core.chart.data.SeriesMarker as ActualSeriesMarker

interface SeriesMarker {

    val instant: Instant

    fun toActualMarker(): ActualSeriesMarker
}

class TradeOrderMarker(
    override val instant: Instant,
    private val orderType: OrderType,
    private val price: BigDecimal,
) : SeriesMarker {

    override fun toActualMarker() = ActualSeriesMarker(
        time = Time.UTCTimestamp(instant.offsetTimeForChart()),
        position = when (orderType) {
            OrderType.Buy -> SeriesMarkerPosition.BelowBar
            OrderType.Sell -> SeriesMarkerPosition.AboveBar
        },
        shape = when (orderType) {
            OrderType.Buy -> SeriesMarkerShape.ArrowUp
            OrderType.Sell -> SeriesMarkerShape.ArrowDown
        },
        color = when (orderType) {
            OrderType.Buy -> Color.Green
            OrderType.Sell -> Color.Red
        },
        text = when (orderType) {
            OrderType.Buy -> price.toPlainString()
            OrderType.Sell -> price.toPlainString()
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
