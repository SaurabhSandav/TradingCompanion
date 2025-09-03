package com.saurabhsandav.trading.backtest

import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import kotlin.time.Instant

data class BacktestOrder<S : BacktestOrder.Status>(
    val id: BacktestOrderId,
    val params: Params,
    val createdAt: Instant,
    val executionType: OrderExecutionType,
    val status: S,
) {

    data class Params(
        val brokerId: BrokerId,
        val instrument: Instrument,
        val symbolId: SymbolId,
        val quantity: KBigDecimal,
        val lots: Int?,
        val side: TradeExecutionSide,
    )

    sealed class Status {

        data class Open(
            val ocoId: Any? = null,
        ) : Status()

        sealed class Closed : Status() {

            abstract val closedAt: Instant
        }

        data class Rejected(
            override val closedAt: Instant,
            val cause: RejectionCause,
        ) : Closed()

        data class Canceled(
            override val closedAt: Instant,
        ) : Closed()

        data class Executed(
            override val closedAt: Instant,
            val executionPrice: KBigDecimal,
        ) : Closed()

        enum class RejectionCause {
            MarginShortfall,
            LessThanMinimumOrderValue,
        }
    }
}

@JvmInline
value class BacktestOrderId(
    val value: Long,
) {

    override fun toString(): String = value.toString()
}

fun BacktestOrder<BacktestOrder.Status.Open>.tryExecute(
    prevPrice: KBigDecimal,
    newPrice: KBigDecimal,
): KBigDecimal? = executionType.tryExecute(
    side = params.side,
    prevPrice = prevPrice,
    newPrice = newPrice,
)
