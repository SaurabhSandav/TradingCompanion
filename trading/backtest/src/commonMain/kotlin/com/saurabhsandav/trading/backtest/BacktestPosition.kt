package com.saurabhsandav.trading.backtest

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeSide

data class BacktestPosition(
    val id: BacktestPositionId,
    val brokerId: BrokerId,
    val instrument: Instrument,
    val symbolId: SymbolId,
    val side: TradeSide,
    val quantity: KBigDecimal,
    val averagePrice: KBigDecimal,
    val pnl: KBigDecimal,
)

@JvmInline
value class BacktestPositionId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}
