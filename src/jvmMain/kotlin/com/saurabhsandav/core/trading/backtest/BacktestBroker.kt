package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trading.backtest.BacktestOrder.*
import com.saurabhsandav.core.trading.backtest.OrderExecution.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

class BacktestBroker(
    private val getCurrentTime: (String) -> LocalDateTime,
) {

    private var nextId = 0L

    private val _openOrders = MutableStateFlow(persistentListOf<OpenOrder>())
    val openOrders = _openOrders.asStateFlow()

    private val _closedOrders = MutableStateFlow(persistentListOf<ClosedOrder>())
    val closedOrders = _closedOrders.asStateFlow()

    fun newOrder(
        params: OrderParams,
        execution: OrderExecution,
        ocoId: Any? = null,
    ): OpenOrder {

        val openOrder = OpenOrder(
            id = nextId++,
            params = params,
            execution = execution,
            createdAt = getCurrentTime(params.ticker),
            ocoId = ocoId,
        )

        // Add to list of orders
        _openOrders.update { it.add(openOrder) }

        return openOrder
    }


    fun cancelOrder(openOrder: OpenOrder) {

        val canceledOrder = ClosedOrder.Canceled(
            id = openOrder.id,
            params = openOrder.params,
            execution = openOrder.execution,
            createdAt = openOrder.createdAt,
            closedAt = getCurrentTime(openOrder.params.ticker),
        )

        // Order is not open anymore
        _openOrders.update { it.remove(openOrder) }

        // Order is closed
        _closedOrders.update { it.add(canceledOrder) }
    }

    fun newPrice(
        ticker: String,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ) {

        // Create copy of open orders to allow removing orders from original list during iteration.
        _openOrders.value
            .filter { it.params.ticker == ticker }
            .forEach { openOrder -> executeOrderIfEligible(openOrder, prevPrice, newPrice) }
    }

    fun reset() {
        _openOrders.update { it.clear() }
        _closedOrders.update { it.clear() }
    }

    private fun executeOrderIfEligible(
        openOrder: OpenOrder,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ) {

        // If order can be executed
        if (openOrder.canExecute(prevPrice, newPrice)) {

            // Order is not open anymore
            _openOrders.update { it.remove(openOrder) }

            val executedOrder = ClosedOrder.Executed(
                id = openOrder.id,
                params = openOrder.params,
                execution = openOrder.execution,
                createdAt = openOrder.createdAt,
                closedAt = getCurrentTime(openOrder.params.ticker),
                executionPrice = when (openOrder.execution) {
                    is Limit -> openOrder.execution.price
                    is Market -> newPrice
                    is StopLimit -> openOrder.execution.limitPrice
                    is StopMarket -> newPrice
                    is TrailingStop -> openOrder.execution.trailingStop
                }
            )

            // Order is closed
            _closedOrders.update { it.add(executedOrder) }

            // Cancel all OCO siblings
            _openOrders
                .value
                .filter { it.ocoId == openOrder.ocoId }
                .forEach { openOCOOrder ->

                    // Order is not open anymore
                    _openOrders.update { it.remove(openOCOOrder) }

                    val canceledOrder = ClosedOrder.Canceled(
                        id = openOCOOrder.id,
                        params = openOCOOrder.params,
                        execution = openOCOOrder.execution,
                        createdAt = openOCOOrder.createdAt,
                        closedAt = getCurrentTime(openOrder.params.ticker),
                    )

                    // Order is closed
                    _closedOrders.update { it.add(canceledOrder) }
                }
        }
    }
}
