package com.saurabhsandav.core.trades

import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.GetOrdersByTrade
import com.saurabhsandav.core.Trade
import com.saurabhsandav.core.TradeOrder
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.utils.brokerage
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

internal class TradeOrdersRepo(
    private val appDB: AppDB,
) {

    val allOrders: Flow<List<TradeOrder>>
        get() = appDB.tradeOrderQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<TradeOrder> {
        return appDB.tradeOrderQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    suspend fun new(
        broker: String,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        type: OrderType,
        price: BigDecimal,
        timestamp: LocalDateTime,
    ) = withContext(Dispatchers.IO) {
        appDB.transaction {

            // Insert Trade order
            appDB.tradeOrderQueries.insert(
                broker = broker,
                ticker = ticker,
                quantity = quantity,
                lots = lots,
                type = type,
                price = price,
                timestamp = timestamp,
            )

            // ID in database of just inserted order
            val orderId = appDB.globalQueries.lastInsertedRowId().executeAsOne()

            // Generate Trade
            val order = appDB.tradeOrderQueries.getById(orderId).executeAsOne()
            consumeOrder(order)
        }
    }

    suspend fun edit(
        id: Long,
        broker: String,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        type: OrderType,
        price: BigDecimal,
        timestamp: LocalDateTime,
    ) = withContext(Dispatchers.IO) {
        appDB.transaction {

            // Update order
            appDB.tradeOrderQueries.update(
                id = id,
                broker = broker,
                ticker = ticker,
                quantity = quantity,
                lots = lots,
                type = type,
                price = price,
                timestamp = timestamp,
            )

            // Order to regenerate trades
            val regenerationOrders = appDB.tradeToOrderMapQueries
                .getOrdersAfterPreviousAllTradesClosed(id)
                .executeAsList()

            // Delete pre-existing trades linked to regenerationOrders
            appDB.tradeToOrderMapQueries.deleteTradesMadeFromOrders(regenerationOrders.map { it.id })

            // Regenerate Trades
            regenerationOrders.forEach(::consumeOrder)
        }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        appDB.transaction {

            // Order to regenerate trades
            val regenerationOrders = appDB.tradeToOrderMapQueries
                .getOrdersAfterPreviousAllTradesClosed(id)
                .executeAsList()

            // Delete pre-existing trades linked to regenerationOrders
            appDB.tradeToOrderMapQueries.deleteTradesMadeFromOrders(regenerationOrders.map { it.id })

            // Delete order
            appDB.tradeOrderQueries.delete(id)

            // Regenerate Trades
            regenerationOrders.filter { it.id != id }.forEach(::consumeOrder)
        }
    }

    fun getOrdersForTrade(id: Long): Flow<List<TradeOrder>> {
        return appDB.tradeToOrderMapQueries.getOrdersByTrade(id).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map(::toTradeOrder)
        }
    }

    private fun consumeOrder(order: TradeOrder) {

        // Currently open trades
        val openTrades = appDB.tradeQueries.getOpenTrades().executeAsList()
        // Trade that will consume this order
        val openTrade = openTrades.find { it.broker == order.broker && it.ticker == order.ticker }

        // No open trade exists to consume order. Create new trade.
        if (openTrade == null) {

            // Insert Trade
            appDB.tradeQueries.insert(
                broker = order.broker,
                ticker = order.ticker,
                instrument = "equity",
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
            )

            // ID in database of just inserted trade
            val tradeId = appDB.globalQueries.lastInsertedRowId().executeAsOne()

            // Link trade and order in database
            appDB.tradeToOrderMapQueries.insert(
                tradeId = tradeId,
                orderId = order.id,
                overrideQuantity = null,
                allTradesClosed = false,
            )

        } else { // Open Trade exists. Update trade with new order

            // Quantity of instrument that is still open after consuming current order
            val currentOpenQuantity = openTrade.quantity - when {
                (openTrade.side == TradeSide.Long && order.type == OrderType.Sell) || (openTrade.side == TradeSide.Short && order.type == OrderType.Buy) -> openTrade.closedQuantity + order.quantity

                else -> openTrade.closedQuantity
            }

            // Get pre-existing orders for open trade
            val orders = appDB.tradeToOrderMapQueries.getOrdersByTrade(openTrade.id).executeAsList()
            val currentOrder = TradeOrder(
                id = -1,
                broker = order.broker,
                ticker = order.ticker,
                quantity = order.quantity,
                lots = order.lots,
                type = order.type,
                price = order.price,
                timestamp = order.timestamp,
            )
            // Recalculate trade parameters after consuming current order
            val trade = (orders.map(::toTradeOrder) + currentOrder).createTrade()

            // Update Trade with new parameters
            appDB.tradeQueries.update(
                id = openTrade.id,
                quantity = trade.quantity,
                closedQuantity = trade.closedQuantity,
                lots = trade.lots,
                averageEntry = trade.averageEntry,
                entryTimestamp = trade.entryTimestamp,
                averageExit = trade.averageExit,
                exitTimestamp = trade.exitTimestamp,
                pnl = trade.pnl,
                fees = trade.fees,
                netPnl = trade.netPnl,
                isClosed = trade.isClosed,
            )

            // If currentOpenQuantity is negative, that means a single order was used to exit a position and create
            // a new position. Create a new trade for this new position
            if (currentOpenQuantity < BigDecimal.ZERO) {

                // Link exiting trade and order in database, while overriding quantity
                appDB.tradeToOrderMapQueries.insert(
                    tradeId = openTrade.id,
                    orderId = order.id,
                    overrideQuantity = order.quantity + currentOpenQuantity,
                    allTradesClosed = false,
                )

                // Quantity for new trade
                val overrideQuantity = currentOpenQuantity.abs()

                // Insert Trade
                appDB.tradeQueries.insert(
                    broker = order.broker,
                    ticker = order.ticker,
                    instrument = "equity",
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
                )

                // ID in database of just inserted trade
                val tradeId = appDB.globalQueries.lastInsertedRowId().executeAsOne()

                // Link new trade and current order, override quantity with remainder quantity after previous trade
                // consumed some
                appDB.tradeToOrderMapQueries.insert(
                    tradeId = tradeId,
                    orderId = order.id,
                    overrideQuantity = overrideQuantity,
                    allTradesClosed = false,
                )
            } else {

                // Link trade and order in database
                appDB.tradeToOrderMapQueries.insert(
                    tradeId = openTrade.id,
                    orderId = order.id,
                    overrideQuantity = null,
                    allTradesClosed = trade.isClosed && !appDB.tradeQueries.anyOpenTrades().executeAsOne(),
                )
            }
        }
    }

    private fun toTradeOrder(orderByTrade: GetOrdersByTrade) = TradeOrder(
        id = orderByTrade.id,
        broker = orderByTrade.broker,
        ticker = orderByTrade.ticker,
        quantity = orderByTrade.overrideQuantity.toBigDecimal(),
        lots = orderByTrade.lots,
        type = orderByTrade.type,
        price = orderByTrade.price,
        timestamp = orderByTrade.timestamp,
    )

    private fun List<TradeOrder>.averagePrice(): BigDecimal {

        val totalQuantity = sumOf { it.quantity }
        val sum: BigDecimal = sumOf { it.price * it.quantity }

        return if (totalQuantity == BigDecimal.ZERO) BigDecimal.ZERO else sum / totalQuantity
    }

    private fun List<TradeOrder>.createTrade(): Trade {

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
                instrument = "equity",
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
            instrument = "equity",
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
}
