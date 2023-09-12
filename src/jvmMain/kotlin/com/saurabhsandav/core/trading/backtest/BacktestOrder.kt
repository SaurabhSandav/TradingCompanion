package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.OrderSide
import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

sealed class BacktestOrder {

    abstract val id: Long
    abstract val params: OrderParams
    abstract val createdAt: LocalDateTime
    abstract val execution: OrderExecution

    data class OrderParams(
        val broker: String,
        val instrument: Instrument,
        val ticker: String,
        val quantity: BigDecimal,
        val lots: Int?,
        val side: OrderSide,
    )

    data class OpenOrder(
        override val id: Long,
        override val params: OrderParams,
        override val createdAt: LocalDateTime,
        override val execution: OrderExecution,
        val ocoId: Any? = null,
    ) : BacktestOrder() {

        fun canExecute(
            prevPrice: BigDecimal,
            newPrice: BigDecimal,
        ): Boolean = execution.canExecute(
            side = params.side,
            prevPrice = prevPrice,
            newPrice = newPrice,
        )
    }

    sealed class ClosedOrder : BacktestOrder() {

        abstract val closedAt: LocalDateTime

        data class Canceled(
            override val id: Long,
            override val params: OrderParams,
            override val createdAt: LocalDateTime,
            override val closedAt: LocalDateTime,
            override val execution: OrderExecution,
        ) : ClosedOrder()

        data class Executed(
            override val id: Long,
            override val params: OrderParams,
            override val createdAt: LocalDateTime,
            override val closedAt: LocalDateTime,
            override val execution: OrderExecution,
            val executionPrice: BigDecimal,
        ) : ClosedOrder()
    }
}
