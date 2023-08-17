package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.utils.brokerage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

internal class TradeOrdersRepo(
    private val tradesDB: TradesDB,
) {

    val allOrders: Flow<List<TradeOrder>>
        get() = tradesDB.tradeOrderQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<TradeOrder> {
        return tradesDB.tradeOrderQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    suspend fun new(
        broker: String,
        instrument: Instrument,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        type: OrderType,
        price: BigDecimal,
        timestamp: LocalDateTime,
        locked: Boolean,
    ): Long = withContext(Dispatchers.IO) {
        tradesDB.transactionWithResult {

            // Insert Trade order
            val orderId = tradesDB.tradeOrderQueries.insert(
                broker = broker,
                instrument = instrument,
                ticker = ticker,
                quantity = quantity,
                lots = lots,
                type = type,
                price = price,
                timestamp = timestamp,
                locked = locked,
            ).executeAsOne()

            // Generate Trade
            val order = tradesDB.tradeOrderQueries.getById(orderId).executeAsOne()
            consumeOrder(order)

            return@transactionWithResult orderId
        }
    }

    suspend fun edit(
        id: Long,
        broker: String,
        instrument: Instrument,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        type: OrderType,
        price: BigDecimal,
        timestamp: LocalDateTime,
    ): Long = withContext(Dispatchers.IO) {

        require(!isLocked(id)) { "Order is locked and cannot be edited" }

        tradesDB.transaction {

            // Update order
            tradesDB.tradeOrderQueries.update(
                id = id,
                broker = broker,
                instrument = instrument,
                ticker = ticker,
                quantity = quantity,
                lots = lots,
                type = type,
                price = price,
                timestamp = timestamp,
            )

            // Trades to be regenerated
            val regenerationTrades = tradesDB.tradeToOrderMapQueries
                .getTradesByOrder(id)
                .executeAsList()

            // Regenerate Trades
            regenerationTrades.forEach { trade ->

                // Get orders for Trade
                val orders = tradesDB.tradeToOrderMapQueries.getOrdersByTrade(trade.id, ::toTradeOrder).executeAsList()

                // Update Trade
                orders.createTrade().updateTradeInDB(trade.id)

                // Regenerate supplemental data
                regenerateSupplementalTradeData(trade.id)
            }
        }

        return@withContext id
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {

        require(!isLocked(id)) { "Order is locked and cannot be deleted" }

        tradesDB.transaction {

            // Trades to be regenerated
            val regenerationTrades = tradesDB.tradeToOrderMapQueries
                .getTradesByOrder(id)
                .executeAsList()

            // Delete order
            tradesDB.tradeOrderQueries.delete(id)

            // Regenerate Trades
            regenerationTrades.forEach { trade ->

                // Get orders for Trade
                val orders = tradesDB.tradeToOrderMapQueries.getOrdersByTrade(trade.id, ::toTradeOrder).executeAsList()

                when {
                    // Delete Trade.
                    orders.isEmpty() -> tradesDB.tradeQueries.delete(trade.id)
                    else -> {

                        // Update Trade
                        orders.createTrade().updateTradeInDB(trade.id)

                        // Regenerate supplemental data
                        regenerateSupplementalTradeData(trade.id)
                    }
                }
            }
        }
    }

    suspend fun lockOrder(id: Long) = withContext(Dispatchers.IO) {
        tradesDB.tradeOrderQueries.lockOrder(id)
    }

    fun getOrdersForTrade(id: Long): Flow<List<TradeOrder>> {
        return tradesDB.tradeToOrderMapQueries.getOrdersByTrade(id, ::toTradeOrder).asFlow().mapToList(Dispatchers.IO)
    }

    fun getOrdersByTickerInInterval(
        ticker: String,
        range: ClosedRange<LocalDateTime>,
    ): Flow<List<TradeOrder>> {
        return tradesDB.tradeOrderQueries
            .getByTickerInInterval(
                ticker = ticker,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getOrdersByTickerAndTradeIdsInInterval(
        ticker: String,
        ids: List<Long>,
        range: ClosedRange<LocalDateTime>,
    ): Flow<List<TradeOrder>> {
        return tradesDB.tradeToOrderMapQueries
            .getOrdersByTickerAndTradeIdsInInterval(
                ticker = ticker,
                ids = ids,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
                mapper = ::toTradeOrder,
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    private suspend fun isLocked(id: Long): Boolean = withContext(Dispatchers.IO) {
        tradesDB.tradeOrderQueries.isLocked(id).executeAsOne()
    }

    private fun consumeOrder(order: TradeOrder) {

        // Currently open trades
        val openTrades = tradesDB.tradeQueries.getOpenTrades().executeAsList()
        // Trade that will consume this order
        val openTrade = openTrades.find {
            it.broker == order.broker && it.instrument == order.instrument && it.ticker == order.ticker
        }

        // No open trade exists to consume order. Create new trade.
        if (openTrade == null) {

            // Insert Trade
            val tradeId = tradesDB.tradeQueries.insert(
                broker = order.broker,
                ticker = order.ticker,
                instrument = order.instrument,
                quantity = order.quantity,
                closedQuantity = BigDecimal.ZERO,
                lots = null,
                side = (if (order.type == OrderType.Buy) TradeSide.Long else TradeSide.Short),
                averageEntry = order.price,
                entryTimestamp = order.timestamp,
                averageExit = null,
                exitTimestamp = null,
                pnl = BigDecimal.ZERO,
                fees = BigDecimal.ZERO,
                netPnl = BigDecimal.ZERO,
                isClosed = false,
            ).executeAsOne()

            // Link trade and order in database
            tradesDB.tradeToOrderMapQueries.insert(
                tradeId = tradeId,
                orderId = order.id,
                overrideQuantity = null,
            )

        } else { // Open Trade exists. Update trade with new order

            // Quantity of instrument that is still open after consuming current order
            val currentOpenQuantity = openTrade.quantity - when {
                (openTrade.side == TradeSide.Long && order.type == OrderType.Sell) ||
                        (openTrade.side == TradeSide.Short && order.type == OrderType.Buy) ->
                    openTrade.closedQuantity + order.quantity

                else -> openTrade.closedQuantity
            }

            // Get pre-existing orders for open trade
            val orders = tradesDB.tradeToOrderMapQueries.getOrdersByTrade(openTrade.id, ::toTradeOrder).executeAsList()

            // Recalculate trade parameters after consuming current order
            val trade = (orders + order).createTrade()

            // Update Trade with new parameters
            trade.updateTradeInDB(openTrade.id)

            // Regenerate supplemental data
            regenerateSupplementalTradeData(openTrade.id)

            // If currentOpenQuantity is negative, that means a single order was used to exit a position and create
            // a new position. Create a new trade for this new position
            if (currentOpenQuantity < BigDecimal.ZERO) {

                // Link exiting trade and order in database, while overriding quantity
                tradesDB.tradeToOrderMapQueries.insert(
                    tradeId = openTrade.id,
                    orderId = order.id,
                    overrideQuantity = order.quantity + currentOpenQuantity,
                )

                // Quantity for new trade
                val overrideQuantity = currentOpenQuantity.abs()

                // Insert Trade
                val tradeId = tradesDB.tradeQueries.insert(
                    broker = order.broker,
                    ticker = order.ticker,
                    instrument = order.instrument,
                    quantity = overrideQuantity,
                    closedQuantity = BigDecimal.ZERO,
                    lots = null,
                    side = (if (order.type == OrderType.Buy) TradeSide.Long else TradeSide.Short),
                    averageEntry = order.price,
                    entryTimestamp = order.timestamp,
                    averageExit = null,
                    exitTimestamp = null,
                    pnl = BigDecimal.ZERO,
                    fees = BigDecimal.ZERO,
                    netPnl = BigDecimal.ZERO,
                    isClosed = false,
                ).executeAsOne()

                // Link new trade and current order, override quantity with remainder quantity after previous trade
                // consumed some
                tradesDB.tradeToOrderMapQueries.insert(
                    tradeId = tradeId,
                    orderId = order.id,
                    overrideQuantity = overrideQuantity,
                )
            } else {

                // Link trade and order in database
                tradesDB.tradeToOrderMapQueries.insert(
                    tradeId = openTrade.id,
                    orderId = order.id,
                    overrideQuantity = null,
                )
            }
        }
    }

    private fun List<TradeOrder>.createTrade(): Trade {

        check(isNotEmpty()) { error("Cannot create trade from empty order list") }

        val firstOrder = first()
        val (entryOrders, exitOrders) = partition { it.type == firstOrder.type }
        val side = if (firstOrder.type == OrderType.Buy) TradeSide.Long else TradeSide.Short
        val entryQuantity = entryOrders.sumOf { it.quantity }
        val exitQuantity = exitOrders.sumOf { it.quantity }
        val lots = entryOrders.mapNotNull { it.lots }.sum()
        val averageEntry = entryOrders.averagePrice()
        val averageExit = when {
            exitOrders.isEmpty() -> null
            else -> {
                val extra = exitQuantity - entryQuantity
                when {
                    extra <= BigDecimal.ZERO -> exitOrders.averagePrice()
                    else -> {
                        (exitOrders.dropLast(1) + exitOrders.last()
                            .copy(quantity = exitOrders.last().quantity - extra)).averagePrice()
                    }
                }
            }
        }
        val closedQuantity = minOf(exitQuantity, entryQuantity)

        val brokerage = averageExit?.let {
            brokerage(
                broker = firstOrder.broker,
                instrument = firstOrder.instrument,
                entry = averageEntry,
                exit = averageExit,
                quantity = closedQuantity,
                side = side,
            )
        }

        return Trade(
            id = -1,
            broker = firstOrder.broker,
            ticker = firstOrder.ticker,
            instrument = firstOrder.instrument,
            quantity = entryQuantity,
            closedQuantity = closedQuantity,
            lots = if (lots == 0) null else lots,
            side = side,
            averageEntry = averageEntry,
            entryTimestamp = firstOrder.timestamp,
            averageExit = averageExit,
            exitTimestamp = exitOrders.lastOrNull()?.timestamp,
            pnl = brokerage?.pnl ?: BigDecimal.ZERO,
            fees = brokerage?.totalCharges ?: BigDecimal.ZERO,
            netPnl = brokerage?.netPNL ?: BigDecimal.ZERO,
            isClosed = (exitQuantity - entryQuantity) >= BigDecimal.ZERO,
        )
    }

    private fun Trade.updateTradeInDB(tradeId: Long) {
        tradesDB.tradeQueries.update(
            id = tradeId,
            quantity = quantity,
            closedQuantity = closedQuantity,
            lots = lots,
            side = side,
            averageEntry = averageEntry,
            entryTimestamp = entryTimestamp,
            averageExit = averageExit,
            exitTimestamp = exitTimestamp,
            pnl = pnl,
            fees = fees,
            netPnl = netPnl,
            isClosed = isClosed,
        )
    }

    private fun List<TradeOrder>.averagePrice(): BigDecimal {

        val totalQuantity = sumOf { it.quantity }
        val sum: BigDecimal = sumOf { it.price * it.quantity }

        return if (totalQuantity == BigDecimal.ZERO) BigDecimal.ZERO else sum / totalQuantity
    }

    private fun toTradeOrder(
        id: Long,
        broker: String,
        instrument: Instrument,
        ticker: String,
        @Suppress("UNUSED_PARAMETER") quantity: BigDecimal,
        lots: Int?,
        type: OrderType,
        price: BigDecimal,
        timestamp: LocalDateTime,
        locked: Boolean,
        overrideQuantity: String,
    ) = TradeOrder(
        id = id,
        broker = broker,
        instrument = instrument,
        ticker = ticker,
        quantity = overrideQuantity.toBigDecimal(),
        lots = lots,
        type = type,
        price = price,
        timestamp = timestamp,
        locked = locked,
    )

    private fun regenerateSupplementalTradeData(tradeId: Long) {

        // Get newly regenerated trade
        val trade = tradesDB.tradeQueries.getById(tradeId).executeAsOne()

        // Get current stops
        val stops = tradesDB.tradeStopQueries.getByTrade(trade.id).executeAsList()

        // Remove stops from DB
        tradesDB.tradeStopQueries.deleteByTrade(trade.id)

        // Save regenerated stops
        stops.forEach { stop ->

            tradesDB.tradeStopQueries.insert(
                tradeId = trade.id,
                price = stop.price,
                risk = when (trade.side) {
                    TradeSide.Long -> trade.averageEntry - stop.price
                    TradeSide.Short -> stop.price - trade.averageEntry
                } * trade.quantity,
            )
        }

        // Get current targets
        val targets = tradesDB.tradeTargetQueries.getByTrade(trade.id).executeAsList()

        // Remove targets from DB
        tradesDB.tradeTargetQueries.deleteByTrade(trade.id)

        // Save regenerated targets
        targets.forEach { target ->

            tradesDB.tradeTargetQueries.insert(
                tradeId = trade.id,
                price = target.price,
                profit = when (trade.side) {
                    TradeSide.Long -> target.price - trade.averageEntry
                    TradeSide.Short -> trade.averageEntry - target.price
                } * trade.quantity
            )
        }

        // Remove MFE and MAE from DB
        tradesDB.tradeMfeMaeQueries.delete(trade.id)
    }
}
