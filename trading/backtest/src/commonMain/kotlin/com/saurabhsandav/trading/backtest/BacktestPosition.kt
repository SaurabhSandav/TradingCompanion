package com.saurabhsandav.trading.backtest

import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeSide
import java.math.BigDecimal

data class BacktestPosition(
    val id: BacktestPositionId,
    val brokerId: BrokerId,
    val instrument: Instrument,
    val symbolId: SymbolId,
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
