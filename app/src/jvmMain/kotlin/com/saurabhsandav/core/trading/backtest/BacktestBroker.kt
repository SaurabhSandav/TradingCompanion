package com.saurabhsandav.core.trading.backtest

import com.saurabhsandav.core.trading.backtest.BacktestOrder.Params
import com.saurabhsandav.core.trading.backtest.BacktestOrder.Status.Canceled
import com.saurabhsandav.core.trading.backtest.BacktestOrder.Status.Executed
import com.saurabhsandav.core.trading.backtest.BacktestOrder.Status.Open
import com.saurabhsandav.core.trading.backtest.BacktestOrder.Status.Rejected
import com.saurabhsandav.core.trading.backtest.BacktestOrder.Status.RejectionCause
import com.saurabhsandav.core.trading.record.model.TradeExecutionSide
import com.saurabhsandav.core.trading.record.model.TradeSide
import com.saurabhsandav.core.utils.Brokerage
import com.saurabhsandav.core.utils.binarySearchByAsResult
import com.saurabhsandav.core.utils.brokerage
import com.saurabhsandav.core.utils.indexOr
import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.isLong
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import kotlin.time.Instant

class BacktestBroker(
    private val account: BacktestAccount,
    private val leverage: BigDecimal = BigDecimal.ONE,
    private val minimumOrderValue: BigDecimal = BigDecimal.ZERO,
    private val minimumMaintenanceMargin: BigDecimal = BigDecimal.ZERO,
    private val onMarginCall: () -> Unit = {},
) {

    var usedMargin: BigDecimal = BigDecimal.ZERO
        private set

    val availableMargin: BigDecimal
        get() = account.balance - usedMargin

    private var nextOrderId = 0L
    private var nextExecutionId = 0L
    private var nextPositionId = 0L
    private var currentInstant = Instant.DISTANT_PAST
    private val currentPrices = mutableMapOf<String, BigDecimal>()

    private val _orders = MutableStateFlow(persistentListOf<BacktestOrder<*>>())
    val orders = _orders.asStateFlow()

    private val _executions = MutableStateFlow(persistentListOf<BacktestExecution>())
    val executions = _executions.asStateFlow()

    private val _positions = MutableStateFlow(persistentListOf<BacktestPosition>())
    val positions = _positions.asStateFlow()

    fun newOrder(
        params: Params,
        executionType: OrderExecutionType,
        ocoId: Any? = null,
    ): BacktestOrderId {

        require(params.quantity > BigDecimal.ZERO) { "BacktestBroker: Quantity must be greater than 0" }

        val (cost, margin) = orderCostAndMargin(params, executionType)

        val newOrder = BacktestOrder(
            id = nextOrderId++.let(::BacktestOrderId),
            params = params,
            executionType = executionType,
            createdAt = currentInstant,
            status = when {
                // Is a close order?
                margin.compareTo(BigDecimal.ZERO) == 0 -> Open(ocoId = ocoId)
                // Is enough margin available?
                margin > availableMargin -> Rejected(currentInstant, RejectionCause.MarginShortfall)
                // Is cost above minimumOrderValue?
                cost < minimumOrderValue -> Rejected(currentInstant, RejectionCause.LessThanMinimumOrderValue)
                else -> Open(ocoId = ocoId)
            },
        )

        // Add to list of orders
        _orders.update { it.add(newOrder) }

        // Update margin
        updateUsedMargin()

        return newOrder.id
    }

    fun cancelOrder(id: BacktestOrderId) {

        // Cancel order if not already closed
        updateOrderStatus(id) {
            if (status !is Open) status else Canceled(closedAt = currentInstant)
        }

        // Update margin
        updateUsedMargin()
    }

    fun newPrice(
        instant: Instant,
        ticker: String,
        price: BigDecimal,
    ) {

        require(currentInstant <= instant) { "Time is in the past" }

        // Set time
        currentInstant = instant

        // Order execution requires previous price. Attempt execution first and then update current price

        // Create copy of open orders to allow removing orders from original list during iteration.
        orders.value
            .filter { it.status is Open && it.params.ticker == ticker }
            .forEach { openOrder ->
                @Suppress("UNCHECKED_CAST")
                executeOrderIfEligible(openOrder as BacktestOrder<Open>, price)
            }

        // Cache current price
        currentPrices[ticker] = price

        // Update margin
        updateUsedMargin()
    }

    private fun executeOrderIfEligible(
        openOrder: BacktestOrder<Open>,
        newPrice: BigDecimal,
    ) {

        val prevPrice = currentPrices[openOrder.params.ticker] ?: return

        // Return if order cannot be executed
        val executionPrice = openOrder.tryExecute(prevPrice, newPrice) ?: return

        val executedOrder = updateOrderStatus(openOrder.id) {

            Executed(
                closedAt = currentInstant,
                executionPrice = executionPrice,
            )
        }

        val execution = BacktestExecution(
            id = nextExecutionId++.let(::BacktestExecutionId),
            broker = executedOrder.params.broker,
            instrument = executedOrder.params.instrument,
            ticker = executedOrder.params.ticker,
            quantity = executedOrder.params.quantity,
            side = executedOrder.params.side,
            price = executedOrder.status.executionPrice,
            timestamp = currentInstant,
        )

        // Add Execution
        _executions.value = executions.value.add(execution)

        // Update positions with execution
        updatePositionsWithExecution(execution)

        // Cancel all OCO siblings
        if (openOrder.status.ocoId != null) {

            orders
                .value
                .filter { it.status is Open && it.status.ocoId == openOrder.status.ocoId }
                .forEach { openOCOOrder ->
                    updateOrderStatus(openOCOOrder.id) { Canceled(closedAt = currentInstant) }
                }
        }
    }

    private fun updateUsedMargin() {

        val positions = positions.value

        // Update PNL for positions
        _positions.value = positions.map { position ->
            position.copy(pnl = position.brokerage().pnl)
        }.toPersistentList()

        @Suppress("UNCHECKED_CAST")
        val openOrders = orders.value.filter { it.status is Open } as List<BacktestOrder<Open>>

        // Margin of positions - Net PNL (If negative)
        val positionMargins = positions.sumOf { position ->

            val turnover = position.averagePrice * position.quantity
            val netPnl = position.brokerage().netPNL

            (turnover / leverage) - netPnl.coerceAtMost(BigDecimal.ZERO)
        }

        // Margin of orders
        val orderMargins = openOrders.sumOf { openOrder ->
            val (_, margin) = orderCostAndMargin(openOrder.params, openOrder.executionType)
            margin
        }

        usedMargin = positionMargins + orderMargins

        if (availableMargin < minimumMaintenanceMargin) onMarginCall()
    }

    private fun orderCostAndMargin(
        params: Params,
        executionType: OrderExecutionType,
    ): Pair<BigDecimal, BigDecimal> {

        // TradeSide for position that can be exited by this order
        val exitsPositionWithSide = when (params.side) {
            TradeExecutionSide.Sell -> TradeSide.Long
            TradeExecutionSide.Buy -> TradeSide.Short
        }

        // Look for a position that can be exited by this order
        val positionToExit = positions.value.find { position ->
            position.ticker == params.ticker && position.side == exitsPositionWithSide
        }

        // Quantity that'll create a new position. Will be 0 if order only closes position.
        val newPositionQuantity = when {
            positionToExit != null -> params.quantity - positionToExit.quantity
            else -> params.quantity
        }

        // This order just closes existing position. 0 margin required.
        if (newPositionQuantity <= BigDecimal.ZERO) return BigDecimal.ZERO to BigDecimal.ZERO

        val executionPrice = when (executionType) {
            is Limit -> executionType.price
            is StopLimit -> executionType.price
            is TrailingStop -> executionType.trailingStop!!
            is Market, is StopMarket -> null
        } ?: getCurrentPrice(params.ticker)

        val cost = executionPrice * newPositionQuantity
        val margin = cost / leverage

        return cost to margin
    }

    private fun updatePositionsWithExecution(execution: BacktestExecution) {

        val currentPrice = getCurrentPrice(execution.ticker)
        val positions = positions.value

        // Position that will consume this execution
        val positionIndex = positions.indexOfFirst {
            it.broker == execution.broker && it.instrument == execution.instrument && it.ticker == execution.ticker
        }.takeIf { it != -1 }

        // No position exists to consume execution. Create new position.
        if (positionIndex == null) {

            val tradeSide = when (execution.side) {
                TradeExecutionSide.Buy -> TradeSide.Long
                TradeExecutionSide.Sell -> TradeSide.Short
            }

            val newPosition = BacktestPosition(
                id = nextPositionId++.let(::BacktestPositionId),
                broker = execution.broker,
                ticker = execution.ticker,
                instrument = execution.instrument,
                quantity = execution.quantity,
                side = tradeSide,
                averagePrice = execution.price,
                pnl = brokerage(
                    broker = execution.broker,
                    instrument = execution.instrument,
                    entry = execution.price,
                    exit = currentPrice,
                    quantity = execution.quantity,
                    side = tradeSide,
                ).pnl,
            )

            // Add position
            _positions.value = positions.add(newPosition)
        } else { // Position exists. Update position with new execution

            val position = positions[positionIndex]

            when {
                // Close position
                (position.side == TradeSide.Long && execution.side == TradeExecutionSide.Sell) ||
                    (position.side == TradeSide.Short && execution.side == TradeExecutionSide.Buy) -> {

                    val extraQuantity = position.quantity - execution.quantity

                    when (extraQuantity.compareTo(BigDecimal.ZERO)) {
                        // Closed fully
                        0 -> {

                            // Remove closed position
                            _positions.value = _positions.value.removeAt(positionIndex)

                            // Update Account
                            account.addTransaction(
                                instant = currentInstant,
                                value = position.brokerage(exit = execution.price).netPNL,
                            )
                        }
                        // Closed Partially
                        1 -> {

                            // Recalculate position parameters after consuming current execution
                            val updatedPosition = position.copy(
                                quantity = extraQuantity,
                                pnl = position.brokerage(
                                    exit = currentPrice,
                                    quantity = extraQuantity,
                                ).pnl,
                            )

                            // Update position with new parameters
                            _positions.value = _positions.value.set(positionIndex, updatedPosition)

                            // Update Account
                            account.addTransaction(
                                instant = currentInstant,
                                value = position.brokerage(
                                    exit = execution.price,
                                    quantity = execution.quantity,
                                ).netPNL,
                            )
                        }
                        // Closed fully and opened new
                        -1 -> {

                            // Remove closed position
                            _positions.value = _positions.value.removeAt(positionIndex)

                            // Update Account
                            account.addTransaction(
                                instant = currentInstant,
                                value = position.brokerage(exit = execution.price).netPNL,
                            )

                            val newPositionSide = when (execution.side) {
                                TradeExecutionSide.Buy -> TradeSide.Long
                                TradeExecutionSide.Sell -> TradeSide.Short
                            }

                            // Add new position
                            val newPosition = BacktestPosition(
                                id = nextPositionId++.let(::BacktestPositionId),
                                broker = execution.broker,
                                ticker = execution.ticker,
                                instrument = execution.instrument,
                                quantity = extraQuantity.negate(),
                                side = newPositionSide,
                                averagePrice = execution.price,
                                pnl = brokerage(
                                    broker = execution.broker,
                                    instrument = execution.instrument,
                                    entry = execution.price,
                                    exit = currentPrice,
                                    quantity = extraQuantity.negate(),
                                    side = newPositionSide,
                                ).pnl,
                            )

                            // Add new position
                            _positions.value = positions.add(newPosition)
                        }
                    }
                }

                // Add to position
                else -> {

                    val newQuantity = position.quantity + execution.quantity
                    val newAveragePrice = run {
                        val positionTurnover = position.averagePrice * position.quantity
                        val executionTurnover = execution.price * execution.quantity
                        (positionTurnover + executionTurnover) / newQuantity
                    }

                    // Recalculate position parameters after consuming current execution
                    val updatedPosition = position.copy(
                        quantity = newQuantity,
                        averagePrice = newAveragePrice,
                        pnl = position.brokerage(
                            entry = newAveragePrice,
                            exit = currentPrice,
                            quantity = newQuantity,
                        ).pnl,
                    )

                    // Update position with new parameters
                    _positions.value = _positions.value.set(positionIndex, updatedPosition)
                }
            }
        }
    }

    private fun <S : BacktestOrder.Status> updateOrderStatus(
        id: BacktestOrderId,
        block: BacktestOrder<*>.() -> S,
    ): BacktestOrder<S> {

        val orders = _orders.value

        val orderIndex = orders.binarySearchByAsResult(id.value) { it.id.value }
            .indexOr { error("Order($id) does not exist") }

        val order = orders[orderIndex]
        val newStatus = order.run(block)

        @Suppress("UNCHECKED_CAST")
        if (order.status == newStatus) return order as BacktestOrder<S>

        val updatedOrder = BacktestOrder(
            id = id,
            params = order.params,
            createdAt = order.createdAt,
            executionType = order.executionType,
            status = newStatus,
        )

        _orders.value = orders.set(orderIndex, updatedOrder)

        return updatedOrder
    }

    private fun BacktestPosition.brokerage(
        entry: BigDecimal = averagePrice,
        exit: BigDecimal = getCurrentPrice(ticker),
        quantity: BigDecimal = this.quantity,
    ): Brokerage = brokerage(
        broker = broker,
        instrument = instrument,
        entry = entry,
        exit = exit,
        quantity = quantity,
        side = side,
    )

    private fun getCurrentPrice(ticker: String): BigDecimal {
        return currentPrices[ticker] ?: error("BacktestBroker: No price available for $ticker")
    }
}

fun BacktestBroker.newCandle(
    ticker: String,
    candle: Candle,
    replayOHLC: Boolean,
) {

    when {
        replayOHLC -> {

            val (extreme1, extreme2) = when {
                candle.isLong -> candle.low to candle.high
                else -> candle.low to candle.high
            }

            newPrice(candle.openInstant, ticker, candle.open)
            newPrice(candle.openInstant, ticker, extreme1)
            newPrice(candle.openInstant, ticker, extreme2)
            newPrice(candle.openInstant, ticker, candle.close)
        }

        else -> newPrice(candle.openInstant, ticker, candle.close)
    }
}
