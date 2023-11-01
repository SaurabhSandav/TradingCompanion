package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import kotlinx.datetime.Instant
import java.math.BigDecimal

sealed class BacktestOrder {

    abstract val id: Long
    abstract val params: OrderParams
    abstract val createdAt: Instant
    abstract val executionType: OrderExecutionType

    data class OrderParams(
        val broker: String,
        val instrument: Instrument,
        val ticker: String,
        val quantity: BigDecimal,
        val lots: Int?,
        val side: TradeExecutionSide,
    )

    data class OpenOrder(
        override val id: Long,
        override val params: OrderParams,
        override val createdAt: Instant,
        override val executionType: OrderExecutionType,
        val ocoId: Any? = null,
    ) : BacktestOrder() {

        fun canExecute(
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean = executionType.canExecute(
            side = params.side,
            prevPrice = prevPrice,
            newPrice = newPrice,
        )
    }

    sealed class ClosedOrder : BacktestOrder() {

        abstract val closedAt: Instant

        data class Canceled(
            override val id: Long,
            override val params: OrderParams,
            override val createdAt: Instant,
            override val closedAt: Instant,
            override val executionType: OrderExecutionType,
        ) : ClosedOrder()

        data class Executed(
            override val id: Long,
            override val params: OrderParams,
            override val createdAt: Instant,
            override val closedAt: Instant,
            override val executionType: OrderExecutionType,
            val executionPrice: BigDecimal,
        ) : ClosedOrder()
    }
}
