package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trading.record.model.Instrument
import com.saurabhsandav.core.trading.record.model.TradeExecutionSide
import java.math.BigDecimal
import kotlin.time.Instant

data class BacktestExecution(
    val id: BacktestExecutionId,
    val broker: String,
    val instrument: Instrument,
    val ticker: String,
    val quantity: BigDecimal,
    val side: TradeExecutionSide,
    val price: BigDecimal,
    val timestamp: Instant,
)

@JvmInline
value class BacktestExecutionId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}
