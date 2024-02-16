package com.saurabhsandav.core.trading.autotrader

import com.saurabhsandav.core.trading.backtest.Limit
import com.saurabhsandav.core.trading.backtest.OrderExecutionType
import com.saurabhsandav.core.trading.backtest.StopMarket
import java.math.BigDecimal

interface OrderTypeToExecutionType {

    fun entry(price: BigDecimal): OrderExecutionType

    fun stop(price: BigDecimal): OrderExecutionType

    fun target(price: BigDecimal): OrderExecutionType

    companion object {

        operator fun invoke(
            entry: (BigDecimal) -> OrderExecutionType = ::Limit,
            stop: (BigDecimal) -> OrderExecutionType = ::StopMarket,
            target: (BigDecimal) -> OrderExecutionType = ::Limit,
        ): OrderTypeToExecutionType = object : OrderTypeToExecutionType {

            override fun entry(price: BigDecimal): OrderExecutionType = entry(price)

            override fun stop(price: BigDecimal): OrderExecutionType = stop(price)

            override fun target(price: BigDecimal): OrderExecutionType = target(price)
        }
    }
}
