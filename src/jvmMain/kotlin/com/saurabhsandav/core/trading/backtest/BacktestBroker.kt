package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.backtest.BacktestOrder.*
import com.saurabhsandav.core.trading.isLong
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import java.math.BigDecimal

class BacktestBroker {

    private var nextId = 0L

    private val _openOrders = MutableStateFlow(persistentListOf<OpenOrder>())
    val openOrders = _openOrders.asStateFlow()

    private val _closedOrders = MutableStateFlow(persistentListOf<ClosedOrder>())
    val closedOrders = _closedOrders.asStateFlow()

    fun newOrder(
        instant: Instant,
        params: OrderParams,
        executionType: OrderExecutionType,
        ocoId: Any? = null,
    ): OpenOrder {

        val openOrder = OpenOrder(
            id = nextId++,
            params = params,
            executionType = executionType,
            createdAt = instant,
            ocoId = ocoId,
        )

        // Add to list of orders
        _openOrders.update { it.add(openOrder) }

        return openOrder
    }

    fun cancelOrder(
        instant: Instant,
        openOrder: OpenOrder,
    ) {

        val canceledOrder = ClosedOrder.Canceled(
            id = openOrder.id,
            params = openOrder.params,
            executionType = openOrder.executionType,
            createdAt = openOrder.createdAt,
            closedAt = instant,
        )

        // Order is not open anymore
        _openOrders.update { it.remove(openOrder) }

        // Order is closed
        _closedOrders.update { it.add(canceledOrder) }
    }

    fun newPrice(
        ticker: String,
        instant: Instant,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ) {

        // Create copy of open orders to allow removing orders from original list during iteration.
        _openOrders.value
            .filter { it.params.ticker == ticker }
            .forEach { openOrder -> executeOrderIfEligible(instant, openOrder, prevPrice, newPrice) }
    }

    fun reset() {
        _openOrders.update { it.clear() }
        _closedOrders.update { it.clear() }
    }

    private fun executeOrderIfEligible(
        instant: Instant,
        openOrder: OpenOrder,
        prevPrice: BigDecimal,
        newPrice: BigDecimal,
    ) {

        val executionPrice = openOrder.tryExecute(prevPrice, newPrice)

        if (executionPrice != null) {

            // Order is not open anymore
            _openOrders.update { it.remove(openOrder) }

            val executedOrder = ClosedOrder.Executed(
                id = openOrder.id,
                params = openOrder.params,
                executionType = openOrder.executionType,
                createdAt = openOrder.createdAt,
                closedAt = instant,
                executionPrice = executionPrice
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
                        executionType = openOCOOrder.executionType,
                        createdAt = openOCOOrder.createdAt,
                        closedAt = instant,
                    )

                    // Order is closed
                    _closedOrders.update { it.add(canceledOrder) }
                }
        }
    }
}

fun BacktestBroker.newCandle(
    ticker: String,
    instant: Instant,
    prevCandle: Candle,
    newCandle: Candle,
    replayOHLC: Boolean,
) {

    when {
        replayOHLC -> {

            val (extreme1, extreme2) = when {
                newCandle.isLong -> newCandle.low to newCandle.high
                else -> newCandle.low to newCandle.high
            }

            newPrice(ticker, instant, prevCandle.close, newCandle.open)
            newPrice(ticker, instant, prevCandle.open, extreme1)
            newPrice(ticker, instant, extreme1, extreme2)
            newPrice(ticker, instant, extreme2, newCandle.close)
        }

        else -> newPrice(ticker, instant, prevCandle.close, newCandle.close)
    }
}
