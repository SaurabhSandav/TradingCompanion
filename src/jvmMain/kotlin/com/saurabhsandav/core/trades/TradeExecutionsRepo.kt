package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.trades.model.Instrument
import com.saurabhsandav.core.trades.model.TradeExecutionSide
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.utils.brokerage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

internal class TradeExecutionsRepo(
    private val tradesDB: TradesDB,
) {

    val allExecutions: Flow<List<TradeExecution>>
        get() = tradesDB.tradeExecutionQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<TradeExecution> {
        return tradesDB.tradeExecutionQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    suspend fun new(
        broker: String,
        instrument: Instrument,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        side: TradeExecutionSide,
        price: BigDecimal,
        timestamp: LocalDateTime,
        locked: Boolean,
    ): Long = withContext(Dispatchers.IO) {
        tradesDB.transactionWithResult {

            // Insert Trade execution
            tradesDB.tradeExecutionQueries.insert(
                broker = broker,
                instrument = instrument,
                ticker = ticker,
                quantity = quantity,
                lots = lots,
                side = side,
                price = price,
                timestamp = timestamp,
                locked = locked,
            )

            // ID in database of just inserted execution
            val executionId = tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne()

            // Generate Trade
            val execution = tradesDB.tradeExecutionQueries.getById(executionId).executeAsOne()
            consumeExecution(execution)

            return@transactionWithResult executionId
        }
    }

    suspend fun edit(
        id: Long,
        broker: String,
        instrument: Instrument,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        side: TradeExecutionSide,
        price: BigDecimal,
        timestamp: LocalDateTime,
    ): Long = withContext(Dispatchers.IO) {

        val notLocked = isLocked(listOf(id)).single().locked.not()

        require(notLocked) { "Trade execution is locked and cannot be edited" }

        tradesDB.transaction {

            // Update execution
            tradesDB.tradeExecutionQueries.update(
                id = id,
                broker = broker,
                instrument = instrument,
                ticker = ticker,
                quantity = quantity,
                lots = lots,
                side = side,
                price = price,
                timestamp = timestamp,
            )

            // Trades to be regenerated
            val regenerationTrades = tradesDB.tradeToExecutionMapQueries
                .getTradesByExecution(id)
                .executeAsList()

            // Regenerate Trades
            regenerationTrades.forEach { trade ->

                // Get executions for Trade
                val executions = tradesDB.tradeToExecutionMapQueries
                    .getExecutionsByTrade(trade.id, ::toTradeExecution)
                    .executeAsList()

                // Update Trade
                executions.createTrade().updateTradeInDB(trade.id)

                // Regenerate supplemental data
                regenerateSupplementalTradeData(trade.id)
            }
        }

        return@withContext id
    }

    suspend fun delete(ids: List<Long>) = withContext(Dispatchers.IO) {

        val noneLocked = isLocked(ids).none { it.locked }

        require(noneLocked) { "Trade execution(s) are locked and cannot be deleted" }

        tradesDB.transaction {

            ids.forEach { id ->

                // Trades to be regenerated
                val regenerationTrades = tradesDB
                    .tradeToExecutionMapQueries
                    .getTradesByExecution(id)
                    .executeAsList()

                // Delete execution
                tradesDB.tradeExecutionQueries.delete(id)

                // Regenerate Trades
                regenerationTrades.forEach { trade ->

                    // Get executions for Trade
                    val executions = tradesDB.tradeToExecutionMapQueries
                        .getExecutionsByTrade(trade.id, ::toTradeExecution)
                        .executeAsList()

                    when {
                        // Delete Trade.
                        executions.isEmpty() -> tradesDB.tradeQueries.delete(trade.id)
                        else -> {

                            // Update Trade
                            executions.createTrade().updateTradeInDB(trade.id)

                            // Regenerate supplemental data
                            regenerateSupplementalTradeData(trade.id)
                        }
                    }
                }
            }
        }
    }

    suspend fun lock(ids: List<Long>) = withContext(Dispatchers.IO) { tradesDB.tradeExecutionQueries.lock(ids) }

    fun getExecutionsForTrade(id: Long): Flow<List<TradeExecution>> {
        return tradesDB.tradeToExecutionMapQueries
            .getExecutionsByTrade(id, ::toTradeExecution)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getExecutionsByTickerInInterval(
        ticker: String,
        range: ClosedRange<LocalDateTime>,
    ): Flow<List<TradeExecution>> {
        return tradesDB.tradeExecutionQueries
            .getByTickerInInterval(
                ticker = ticker,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getExecutionsByTickerAndTradeIdsInInterval(
        ticker: String,
        ids: List<Long>,
        range: ClosedRange<LocalDateTime>,
    ): Flow<List<TradeExecution>> {
        return tradesDB.tradeToExecutionMapQueries
            .getExecutionsByTickerAndTradeIdsInInterval(
                ticker = ticker,
                ids = ids,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
                mapper = ::toTradeExecution,
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    private suspend fun isLocked(ids: List<Long>) = withContext(Dispatchers.IO) {
        tradesDB.tradeExecutionQueries.isLocked(ids).executeAsList()
    }

    private fun consumeExecution(execution: TradeExecution) {

        // Currently open trades
        val openTrades = tradesDB.tradeQueries.getOpenTrades().executeAsList()
        // Trade that will consume this execution
        val openTrade = openTrades.find {
            it.broker == execution.broker && it.instrument == execution.instrument && it.ticker == execution.ticker
        }

        // No open trade exists to consume execution. Create new trade.
        if (openTrade == null) {

            // Insert Trade
            tradesDB.tradeQueries.insert(
                broker = execution.broker,
                ticker = execution.ticker,
                instrument = execution.instrument,
                quantity = execution.quantity,
                closedQuantity = BigDecimal.ZERO,
                lots = null,
                side = (if (execution.side == TradeExecutionSide.Buy) TradeSide.Long else TradeSide.Short),
                averageEntry = execution.price,
                entryTimestamp = execution.timestamp,
                averageExit = null,
                exitTimestamp = null,
                pnl = BigDecimal.ZERO,
                fees = BigDecimal.ZERO,
                netPnl = BigDecimal.ZERO,
                isClosed = false,
            )

            // ID in database of just inserted trade
            val tradeId = tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne()

            // Link trade and execution in database
            tradesDB.tradeToExecutionMapQueries.insert(
                tradeId = tradeId,
                executionId = execution.id,
                overrideQuantity = null,
            )

        } else { // Open Trade exists. Update trade with new execution

            // Quantity of instrument that is still open after consuming current execution
            val currentOpenQuantity = openTrade.quantity - when {
                (openTrade.side == TradeSide.Long && execution.side == TradeExecutionSide.Sell) ||
                        (openTrade.side == TradeSide.Short && execution.side == TradeExecutionSide.Buy) ->
                    openTrade.closedQuantity + execution.quantity

                else -> openTrade.closedQuantity
            }

            // Get pre-existing executions for open trade
            val executions = tradesDB
                .tradeToExecutionMapQueries
                .getExecutionsByTrade(openTrade.id, ::toTradeExecution)
                .executeAsList()

            // Recalculate trade parameters after consuming current execution
            val trade = (executions + execution).createTrade()

            // Update Trade with new parameters
            trade.updateTradeInDB(openTrade.id)

            // Regenerate supplemental data
            regenerateSupplementalTradeData(openTrade.id)

            // If currentOpenQuantity is negative, that means a single execution was used to exit a position and create
            // a new position. Create a new trade for this new position
            if (currentOpenQuantity < BigDecimal.ZERO) {

                // Link existing trade and execution in database, while overriding quantity
                tradesDB.tradeToExecutionMapQueries.insert(
                    tradeId = openTrade.id,
                    executionId = execution.id,
                    overrideQuantity = execution.quantity + currentOpenQuantity,
                )

                // Quantity for new trade
                val overrideQuantity = currentOpenQuantity.abs()

                // Insert Trade
                tradesDB.tradeQueries.insert(
                    broker = execution.broker,
                    ticker = execution.ticker,
                    instrument = execution.instrument,
                    quantity = overrideQuantity,
                    closedQuantity = BigDecimal.ZERO,
                    lots = null,
                    side = (if (execution.side == TradeExecutionSide.Buy) TradeSide.Long else TradeSide.Short),
                    averageEntry = execution.price,
                    entryTimestamp = execution.timestamp,
                    averageExit = null,
                    exitTimestamp = null,
                    pnl = BigDecimal.ZERO,
                    fees = BigDecimal.ZERO,
                    netPnl = BigDecimal.ZERO,
                    isClosed = false,
                )

                // ID in database of just inserted trade
                val tradeId = tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne()

                // Link new trade and current execution, override quantity with remainder quantity after previous trade
                // consumed some
                tradesDB.tradeToExecutionMapQueries.insert(
                    tradeId = tradeId,
                    executionId = execution.id,
                    overrideQuantity = overrideQuantity,
                )
            } else {

                // Link trade and execution in database
                tradesDB.tradeToExecutionMapQueries.insert(
                    tradeId = openTrade.id,
                    executionId = execution.id,
                    overrideQuantity = null,
                )
            }
        }
    }

    private fun List<TradeExecution>.createTrade(): Trade {

        check(isNotEmpty()) { error("Cannot create trade without any executions") }

        val firstExecution = first()
        val (entryExecutions, exitExecutions) = partition { it.side == firstExecution.side }
        val side = if (firstExecution.side == TradeExecutionSide.Buy) TradeSide.Long else TradeSide.Short
        val entryQuantity = entryExecutions.sumOf { it.quantity }
        val exitQuantity = exitExecutions.sumOf { it.quantity }
        val lots = entryExecutions.mapNotNull { it.lots }.sum()
        val averageEntry = entryExecutions.averagePrice()
        val averageExit = when {
            exitExecutions.isEmpty() -> null
            else -> {
                val extra = exitQuantity - entryQuantity
                when {
                    extra <= BigDecimal.ZERO -> exitExecutions.averagePrice()
                    else -> {
                        (exitExecutions.dropLast(1) + exitExecutions.last()
                            .copy(quantity = exitExecutions.last().quantity - extra)).averagePrice()
                    }
                }
            }
        }
        val closedQuantity = minOf(exitQuantity, entryQuantity)

        val brokerage = averageExit?.let {
            brokerage(
                broker = firstExecution.broker,
                instrument = firstExecution.instrument,
                entry = averageEntry,
                exit = averageExit,
                quantity = closedQuantity,
                side = side,
            )
        }

        return Trade(
            id = -1,
            broker = firstExecution.broker,
            ticker = firstExecution.ticker,
            instrument = firstExecution.instrument,
            quantity = entryQuantity,
            closedQuantity = closedQuantity,
            lots = if (lots == 0) null else lots,
            side = side,
            averageEntry = averageEntry,
            entryTimestamp = firstExecution.timestamp,
            averageExit = averageExit,
            exitTimestamp = exitExecutions.lastOrNull()?.timestamp,
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

    private fun List<TradeExecution>.averagePrice(): BigDecimal {

        val totalQuantity = sumOf { it.quantity }
        val sum: BigDecimal = sumOf { it.price * it.quantity }

        return if (totalQuantity == BigDecimal.ZERO) BigDecimal.ZERO else sum / totalQuantity
    }

    private fun toTradeExecution(
        id: Long,
        broker: String,
        instrument: Instrument,
        ticker: String,
        @Suppress("UNUSED_PARAMETER") quantity: BigDecimal,
        lots: Int?,
        side: TradeExecutionSide,
        price: BigDecimal,
        timestamp: LocalDateTime,
        locked: Boolean,
        overrideQuantity: String,
    ) = TradeExecution(
        id = id,
        broker = broker,
        instrument = instrument,
        ticker = ticker,
        quantity = overrideQuantity.toBigDecimal(),
        lots = lots,
        side = side,
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
            )
        }

        // Remove MFE and MAE from DB
        tradesDB.tradeMfeMaeQueries.delete(trade.id)
    }
}
