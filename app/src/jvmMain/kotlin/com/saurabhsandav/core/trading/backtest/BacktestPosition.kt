package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeSide
import java.math.BigDecimal

data class BacktestPosition(
    val id: BacktestPositionId,
    val broker: String,
    val instrument: Instrument,
    val ticker: String,
    val side: TradeSide,
    val quantity: BigDecimal,
    val averagePrice: BigDecimal,
    val pnl: BigDecimal,
)

@JvmInline
value class BacktestPositionId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}
