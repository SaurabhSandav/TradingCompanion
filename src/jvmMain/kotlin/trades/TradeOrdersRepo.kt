package trades

import AppModule
import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.GetOrdersByTrade
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import model.Side
import trades.model.OrderType
import trades.model.Trade
import trades.model.TradeOrder
import utils.brokerage
import java.math.BigDecimal
import kotlin.math.absoluteValue

internal class TradeOrdersRepo(
    appModule: AppModule,
    private val appDB: AppDB = appModule.appDB,
) {

    val allOrders: Flow<List<TradeOrder>>
        get() = appDB.tradeOrderQueries.getAll(::dbOrderToOrderMapper).asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<TradeOrder> {
        return appDB.tradeOrderQueries.getById(id, ::dbOrderToOrderMapper).asFlow().mapToOne(Dispatchers.IO)
    }

    suspend fun new(
        broker: String,
        ticker: String,
        quantity: Int,
        lots: Int?,
        type: OrderType,
        price: BigDecimal,
        timestamp: LocalDateTime,
    ) = withContext(Dispatchers.IO) {
        appDB.transaction {

            // Insert Trade order
            appDB.tradeOrderQueries.insert(
                id = null,
                broker = broker,
                ticker = ticker,
                quantity = quantity.toString(),
                lots = lots,
                type = type.strValue,
                price = price.toPlainString(),
                timestamp = timestamp.toString(),
            )

            // ID in database of just inserted order
            val orderId = appDB.globalQueries.lastInsertedRowId().executeAsOne()

            // Generate Trade
            val order = appDB.tradeOrderQueries.getById(orderId, ::dbOrderToOrderMapper).executeAsOne()
            consumeOrder(order)
        }
    }

    suspend fun edit(
        id: Long,
        broker: String,
        ticker: String,
        quantity: Int,
        lots: Int?,
        type: OrderType,
        price: BigDecimal,
        timestamp: LocalDateTime,
    ) = withContext(Dispatchers.IO) {
        appDB.transaction {

            // Update order
            appDB.tradeOrderQueries.insert(
                id = id,
                broker = broker,
                ticker = ticker,
                quantity = quantity.toString(),
                lots = lots,
                type = type.strValue,
                price = price.toPlainString(),
                timestamp = timestamp.toString(),
            )

            // Order to regenerate trades
            val regenerationOrders = appDB.tradeToOrderMapQueries
                .getOrdersAfterPreviousAllTradesClosed(id, ::dbOrderToOrderMapper)
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
                .getOrdersAfterPreviousAllTradesClosed(id, ::dbOrderToOrderMapper)
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
        val openTrades = appDB.tradeQueries.getOpenTrades(::dbTradeToTradeMapper).executeAsList()
        // Trade that will consume this order
        val openTrade = openTrades.find { it.broker == order.broker && it.ticker == order.ticker }

        // No open trade exists to consume order. Create new trade.
        if (openTrade == null) {

            // Insert Trade
            appDB.tradeQueries.insert(
                id = null,
                broker = order.broker,
                ticker = order.ticker,
                instrument = "equity",
                quantity = order.quantity.toString(),
                closedQuantity = 0.toString(),
                lots = null,
                side = (if (order.type == OrderType.Buy) Side.Long else Side.Short).toString(),
                averageEntry = order.price.toPlainString(),
                entryTimestamp = order.timestamp.toString(),
                averageExit = null,
                exitTimestamp = null,
                pnl = BigDecimal.ZERO.toPlainString(),
                fees = BigDecimal.ZERO.toPlainString(),
                netPnl = BigDecimal.ZERO.toPlainString(),
                isClosed = false.toString(),
            )

            // ID in database of just inserted trade
            val tradeId = appDB.globalQueries.lastInsertedRowId().executeAsOne()

            // Link trade and order in database
            appDB.tradeToOrderMapQueries.insert(
                tradeId = tradeId,
                orderId = order.id,
                overrideQuantity = null,
                allTradesClosed = false.toString(),
            )

        } else { // Open Trade exists. Update trade with new order

            // Quantity of instrument that is still open after consuming current order
            val currentOpenQuantity = openTrade.quantity - when {
                (openTrade.side == Side.Long && order.type == OrderType.Sell) || (openTrade.side == Side.Short && order.type == OrderType.Buy) -> openTrade.closedQuantity + order.quantity

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
                quantity = trade.quantity.toString(),
                closedQuantity = trade.closedQuantity.toString(),
                lots = trade.lots,
                averageEntry = trade.averageEntry.toPlainString(),
                entryTimestamp = trade.entryTimestamp.toString(),
                averageExit = trade.averageExit?.toPlainString(),
                exitTimestamp = trade.exitTimestamp?.toString(),
                pnl = trade.pnl.toString(),
                fees = trade.fees.toString(),
                netPnl = trade.netPnl.toString(),
                isClosed = trade.isClosed.toString(),
            )

            // If currentOpenQuantity is negative, that means a single order was used to exit a position and create
            // a new position. Create a new trade for this new position
            if (currentOpenQuantity < 0) {

                // Link exiting trade and order in database, while overriding quantity
                appDB.tradeToOrderMapQueries.insert(
                    tradeId = openTrade.id,
                    orderId = order.id,
                    overrideQuantity = (order.quantity + currentOpenQuantity).toString(),
                    allTradesClosed = false.toString(),
                )

                // Quantity for new trade
                val overrideQuantity = currentOpenQuantity.absoluteValue

                // Insert Trade
                appDB.tradeQueries.insert(
                    id = null,
                    broker = order.broker,
                    ticker = order.ticker,
                    instrument = "equity",
                    quantity = overrideQuantity.toString(),
                    closedQuantity = 0.toString(),
                    lots = null,
                    side = (if (order.type == OrderType.Buy) Side.Long else Side.Short).toString(),
                    averageEntry = order.price.toPlainString(),
                    entryTimestamp = order.timestamp.toString(),
                    averageExit = null,
                    exitTimestamp = null,
                    pnl = BigDecimal.ZERO.toPlainString(),
                    fees = BigDecimal.ZERO.toPlainString(),
                    netPnl = BigDecimal.ZERO.toPlainString(),
                    isClosed = false.toString(),
                )

                // ID in database of just inserted trade
                val tradeId = appDB.globalQueries.lastInsertedRowId().executeAsOne()

                // Link new trade and current order, override quantity with remainder quantity after previous trade
                // consumed some
                appDB.tradeToOrderMapQueries.insert(
                    tradeId = tradeId,
                    orderId = order.id,
                    overrideQuantity = overrideQuantity.toString(),
                    allTradesClosed = false.toString(),
                )
            } else {

                // Link trade and order in database
                appDB.tradeToOrderMapQueries.insert(
                    tradeId = openTrade.id,
                    orderId = order.id,
                    overrideQuantity = null,
                    allTradesClosed = (trade.isClosed && !appDB.tradeQueries.anyOpenTrades()
                        .executeAsOne()).toString(),
                )
            }
        }
    }

    private fun dbOrderToOrderMapper(
        id: Long,
        broker: String,
        ticker: String,
        quantity: String,
        lots: Int?,
        type: String,
        price: String,
        timestamp: String,
    ) = TradeOrder(
        id = id,
        broker = broker,
        ticker = ticker,
        quantity = quantity.toInt(),
        lots = lots,
        type = OrderType.fromString(type),
        price = price.toBigDecimal(),
        timestamp = LocalDateTime.parse(timestamp),
    )

    private fun toTradeOrder(orderByTrade: GetOrdersByTrade) = TradeOrder(
        id = orderByTrade.id,
        broker = orderByTrade.broker,
        ticker = orderByTrade.ticker,
        quantity = orderByTrade.overrideQuantity.toInt(),
        lots = orderByTrade.lots,
        type = OrderType.fromString(orderByTrade.type),
        price = orderByTrade.price.toBigDecimal(),
        timestamp = LocalDateTime.parse(orderByTrade.timestamp),
    )

    private fun List<TradeOrder>.averagePrice(): BigDecimal {

        val totalQuantity = sumOf { it.quantity }
        val sum: BigDecimal = sumOf { it.price * it.quantity.toBigDecimal() }

        return if (totalQuantity == 0) BigDecimal.ZERO else sum / totalQuantity.toBigDecimal()
    }

    private fun List<TradeOrder>.createTrade(): Trade {

        val firstOrder = first()
        val (entryOrders, exitOrders) = partition { it.type == firstOrder.type }
        val side = if (firstOrder.type == OrderType.Buy) Side.Long else Side.Short
        val entryQuantity = entryOrders.sumOf { it.quantity }
        val exitQuantity = exitOrders.sumOf { it.quantity }
        val lots = entryOrders.mapNotNull { it.lots }.sum()
        val averageEntry = entryOrders.averagePrice()
        val averageExit = when {
            exitOrders.isEmpty() -> null
            else -> {
                val extra = exitQuantity - entryQuantity
                when {
                    extra <= 0 -> exitOrders.averagePrice()
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
                quantity = closedQuantity.toBigDecimal(),
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
            isClosed = (exitQuantity - entryQuantity) >= 0,
        )
    }
}
