package com.saurabhsandav.core.trades

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.thirdparty.sqldelight_paging.QueryPagingSource
import com.saurabhsandav.core.trades.model.*
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.brokerage
import com.saurabhsandav.core.utils.withoutNanoseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.math.MathContext
import java.nio.file.Path
import kotlin.io.path.deleteExisting

internal class Executions(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
    private val attachmentsPath: Path,
    private val onTradesUpdated: suspend () -> Unit,
) {

    suspend fun new(
        broker: String,
        instrument: Instrument,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        side: TradeExecutionSide,
        price: BigDecimal,
        timestamp: Instant,
        locked: Boolean,
    ): TradeExecutionId = withContext(appDispatchers.IO) {

        val executionId = tradesDB.transactionWithResult {

            // Insert Trade execution
            tradesDB.tradeExecutionQueries.insert(
                broker = broker,
                instrument = instrument,
                ticker = ticker,
                quantity = quantity.stripTrailingZeros(),
                lots = lots,
                side = side,
                price = price.stripTrailingZeros(),
                timestamp = timestamp.withoutNanoseconds(),
                locked = locked,
            )

            // ID in database of just inserted execution
            val executionId = tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::TradeExecutionId)

            // Generate Trade
            val execution = tradesDB.tradeExecutionQueries.getById(executionId).executeAsOne()
            consumeExecution(execution)

            return@transactionWithResult executionId
        }

        // Notify updates
        onTradesUpdated()

        return@withContext executionId
    }

    suspend fun edit(
        id: TradeExecutionId,
        broker: String,
        instrument: Instrument,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        side: TradeExecutionSide,
        price: BigDecimal,
        timestamp: Instant,
    ): Unit = withContext(appDispatchers.IO) {

        val notLocked = isLocked(listOf(id)).single().locked.not()

        require(notLocked) { "TradeExecution($id) is locked and cannot be edited" }

        tradesDB.transaction {

            // Update execution
            tradesDB.tradeExecutionQueries.update(
                id = id,
                broker = broker,
                instrument = instrument,
                ticker = ticker,
                quantity = quantity.stripTrailingZeros(),
                lots = lots,
                side = side,
                price = price.stripTrailingZeros(),
                timestamp = timestamp.withoutNanoseconds(),
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

        // Notify updates
        onTradesUpdated()
    }

    suspend fun delete(ids: List<TradeExecutionId>) = withContext(appDispatchers.IO) {

        val noneLocked = isLocked(ids).none { it.locked }

        require(noneLocked) { "TradeExecution(s) are locked and cannot be deleted" }

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
                        executions.isEmpty() -> {

                            // Delete Trade.
                            tradesDB.tradeQueries.delete(trade.id)

                            // Delete supplemental data
                            deleteSupplementalTradeData()
                        }

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

        // Notify updates
        onTradesUpdated()
    }

    suspend fun lock(ids: List<TradeExecutionId>) = withContext(appDispatchers.IO) {
        tradesDB.tradeExecutionQueries.lock(ids)
    }

    fun getById(id: TradeExecutionId): Flow<TradeExecution> {
        return tradesDB.tradeExecutionQueries
            .getById(id)
            .asFlow()
            .mapToOneOrNull(appDispatchers.IO)
            .map { it ?: error("TradeExecution($id) not found") }
    }

    fun getTodayCount(): Flow<Long> {
        return tradesDB.tradeExecutionQueries.getTodayCount().asFlow().mapToOne(appDispatchers.IO)
    }

    fun getBeforeTodayCount(): Flow<Long> {
        return tradesDB.tradeExecutionQueries.getBeforeTodayCount().asFlow().mapToOne(appDispatchers.IO)
    }

    fun getAllPagingSource(): PagingSource<Int, TradeExecution> = QueryPagingSource(
        countQuery = tradesDB.tradeExecutionQueries.getAllCount(),
        transacter = tradesDB.tradeExecutionQueries,
        context = appDispatchers.IO,
        queryProvider = { limit, offset ->

            tradesDB.tradeExecutionQueries.getAllPaged(
                limit = limit,
                offset = offset,
            )
        },
    )

    fun getForTrade(id: TradeId): Flow<List<TradeExecution>> {
        return tradesDB.tradeToExecutionMapQueries
            .getExecutionsByTrade(id, ::toTradeExecution)
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    fun getByTickerInInterval(
        ticker: String,
        range: ClosedRange<Instant>,
    ): Flow<List<TradeExecution>> {
        return tradesDB.tradeExecutionQueries
            .getByTickerInInterval(
                ticker = ticker,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(appDispatchers.IO)
    }

    fun getByTickerAndTradeIdsInInterval(
        ticker: String,
        ids: List<TradeId>,
        range: ClosedRange<Instant>,
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
            .mapToList(appDispatchers.IO)
    }

    private suspend fun isLocked(ids: List<TradeExecutionId>) = withContext(appDispatchers.IO) {
        tradesDB.tradeExecutionQueries.isLocked(ids).executeAsList()
    }

    private fun consumeExecution(execution: TradeExecution) {

        // Currently open trades
        val openTrades = tradesDB.tradeQueries.getOpen().executeAsList()
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
                quantity = execution.quantity.stripTrailingZeros(),
                closedQuantity = BigDecimal.ZERO,
                lots = null,
                side = (if (execution.side == TradeExecutionSide.Buy) TradeSide.Long else TradeSide.Short),
                averageEntry = execution.price.stripTrailingZeros(),
                entryTimestamp = execution.timestamp.withoutNanoseconds(),
                averageExit = null,
                exitTimestamp = null,
                pnl = BigDecimal.ZERO,
                fees = BigDecimal.ZERO,
                netPnl = BigDecimal.ZERO,
                isClosed = false,
            )

            // ID in database of just inserted trade
            val tradeId = tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::TradeId)

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
                    overrideQuantity = (execution.quantity + currentOpenQuantity).stripTrailingZeros(),
                )

                // Quantity for new trade
                val overrideQuantity = currentOpenQuantity.abs()

                // Insert Trade
                tradesDB.tradeQueries.insert(
                    broker = execution.broker,
                    ticker = execution.ticker,
                    instrument = execution.instrument,
                    quantity = overrideQuantity.stripTrailingZeros(),
                    closedQuantity = BigDecimal.ZERO,
                    lots = null,
                    side = (if (execution.side == TradeExecutionSide.Buy) TradeSide.Long else TradeSide.Short),
                    averageEntry = execution.price.stripTrailingZeros(),
                    entryTimestamp = execution.timestamp.withoutNanoseconds(),
                    averageExit = null,
                    exitTimestamp = null,
                    pnl = BigDecimal.ZERO,
                    fees = BigDecimal.ZERO,
                    netPnl = BigDecimal.ZERO,
                    isClosed = false,
                )

                // ID in database of just inserted trade
                val tradeId = tradesDB.tradesDBUtilsQueries.lastInsertedRowId().executeAsOne().let(::TradeId)

                // Link new trade and current execution, override quantity with remainder quantity after previous trade
                // consumed some
                tradesDB.tradeToExecutionMapQueries.insert(
                    tradeId = tradeId,
                    executionId = execution.id,
                    overrideQuantity = overrideQuantity.stripTrailingZeros(),
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
            id = TradeId(-1),
            broker = firstExecution.broker,
            ticker = firstExecution.ticker,
            instrument = firstExecution.instrument,
            quantity = entryQuantity,
            closedQuantity = closedQuantity,
            lots = if (lots == 0) null else lots,
            side = side,
            averageEntry = averageEntry,
            entryTimestamp = firstExecution.timestamp.withoutNanoseconds(),
            averageExit = averageExit,
            exitTimestamp = exitExecutions.lastOrNull()?.timestamp?.withoutNanoseconds(),
            pnl = brokerage?.pnl ?: BigDecimal.ZERO,
            fees = brokerage?.totalCharges ?: BigDecimal.ZERO,
            netPnl = brokerage?.netPNL ?: BigDecimal.ZERO,
            isClosed = (exitQuantity - entryQuantity) >= BigDecimal.ZERO,
        )
    }

    private fun Trade.updateTradeInDB(tradeId: TradeId) {

        tradesDB.tradeQueries.update(
            id = tradeId,
            quantity = quantity.stripTrailingZeros(),
            closedQuantity = closedQuantity.stripTrailingZeros(),
            lots = lots,
            side = side,
            averageEntry = averageEntry.stripTrailingZeros(),
            entryTimestamp = entryTimestamp.withoutNanoseconds(),
            averageExit = averageExit?.stripTrailingZeros(),
            exitTimestamp = exitTimestamp?.withoutNanoseconds(),
            pnl = pnl.stripTrailingZeros(),
            fees = fees.stripTrailingZeros(),
            netPnl = netPnl.stripTrailingZeros(),
            isClosed = isClosed,
        )
    }

    private fun List<TradeExecution>.averagePrice(): BigDecimal {

        val totalQuantity = sumOf { it.quantity }
        val sum = sumOf { it.price * it.quantity }

        return when (totalQuantity.compareTo(BigDecimal.ZERO)) {
            0 -> BigDecimal.ZERO
            else -> sum.divide(totalQuantity, MathContext.DECIMAL32)
        }
    }

    private fun toTradeExecution(
        id: TradeExecutionId,
        broker: String,
        instrument: Instrument,
        ticker: String,
        quantity: BigDecimal,
        lots: Int?,
        side: TradeExecutionSide,
        price: BigDecimal,
        timestamp: Instant,
        locked: Boolean,
        overrideQuantity: BigDecimal?,
    ) = TradeExecution(
        id = id,
        broker = broker,
        instrument = instrument,
        ticker = ticker,
        quantity = overrideQuantity ?: quantity,
        lots = lots,
        side = side,
        price = price,
        timestamp = timestamp,
        locked = locked,
    )

    private fun regenerateSupplementalTradeData(tradeId: TradeId) {

        /*
        * - Stops -> No action required
        * - Targets -> No action required
        * - Notes -> No action required
        * - Attachments -> No action required
        * - Excursions -> Delete, Will automatically regenerate through scheduled job
        * */

        // Remove Excursions from DB
        tradesDB.tradeExcursionsQueries.delete(tradeId)
    }

    private fun deleteSupplementalTradeData() {

        /*
        * - Stops -> Cascade deleted in SQL
        * - Targets -> Cascade deleted in SQL
        * - Notes -> Cascade deleted in SQL
        * - Attachments -> Cascade deleted in SQL, Delete orphaned AttachmentFile(s)
        * - Excursions -> Cascade deleted in SQL
        * */

        // Delete orphaned AttachmentFiles
        with(tradesDB.attachmentFileQueries) {

            // Delete files
            getOrphaned()
                .executeAsList()
                .forEach { attachmentsPath.resolve(it.fileName).deleteExisting() }

            deleteOrphaned()
        }
    }
}
