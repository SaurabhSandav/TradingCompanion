package com.saurabhsandav.trading.record

import androidx.paging.PagingSource
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.kbigdecimal.KMathContext
import com.saurabhsandav.kbigdecimal.sumOf
import com.saurabhsandav.paging.pagingsource.QueryPagingSource
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.core.Instrument
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.TradeExecutionId
import com.saurabhsandav.trading.record.model.TradeExecutionSide
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSide
import com.saurabhsandav.trading.record.model.isLong
import com.saurabhsandav.trading.record.utils.withoutNanoseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.collections.sumOf
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.deleteExisting
import kotlin.math.absoluteValue
import kotlin.time.Instant

class Executions(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
    private val attachmentsDir: Path?,
    private val brokerProvider: BrokerProvider,
    private val getSymbol: (suspend (BrokerId, SymbolId) -> Symbol?)?,
    private val onTradesUpdated: suspend () -> Unit,
) {

    suspend fun new(
        brokerId: BrokerId,
        instrument: Instrument,
        symbolId: SymbolId,
        quantity: KBigDecimal,
        lots: Int,
        side: TradeExecutionSide,
        price: KBigDecimal,
        timestamp: Instant,
        locked: Boolean,
    ): TradeExecutionId = withContext(coroutineContext) {

        val broker = brokerProvider.getBroker(brokerId)
        val symbol = getSymbol?.invoke(brokerId, symbolId)

        if (symbol != null) validateQuantity(quantity, symbol)

        val executionId = tradesDB.transactionWithResult {

            // Add Broker
            tradesDB.brokerQueries.insert(
                id = brokerId,
                name = broker.name,
            )

            // Add Symbol
            if (symbol != null) {
                tradesDB.symbolQueries.insert(symbol)
            }

            // Insert Trade execution
            val executionId = tradesDB.tradeExecutionQueries.insert(
                brokerId = brokerId,
                instrument = instrument,
                symbolId = symbolId,
                quantity = quantity,
                lots = lots,
                side = side,
                price = price,
                timestamp = timestamp.withoutNanoseconds(),
                locked = locked,
            ).executeAsOne()

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
        brokerId: BrokerId,
        instrument: Instrument,
        symbolId: SymbolId,
        quantity: KBigDecimal,
        lots: Int,
        side: TradeExecutionSide,
        price: KBigDecimal,
        timestamp: Instant,
    ): Unit = withContext(coroutineContext) {

        val notLocked = isLocked(listOf(id)).single().locked.not()

        require(notLocked) { "TradeExecution($id) is locked and cannot be edited" }

        val broker = brokerProvider.getBroker(brokerId)
        val symbol = getSymbol?.invoke(brokerId, symbolId)

        if (symbol != null) validateQuantity(quantity, symbol)

        tradesDB.transaction {

            // Add Broker
            tradesDB.brokerQueries.insert(
                id = brokerId,
                name = broker.name,
            )

            // Add Symbol
            if (symbol != null) {
                tradesDB.symbolQueries.insert(symbol)
            }

            // Update execution
            tradesDB.tradeExecutionQueries.update(
                id = id,
                brokerId = brokerId,
                instrument = instrument,
                symbolId = symbolId,
                quantity = quantity,
                lots = lots,
                side = side,
                price = price,
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

    suspend fun delete(ids: List<TradeExecutionId>) = withContext(coroutineContext) {

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

    suspend fun lock(ids: List<TradeExecutionId>) = withContext(coroutineContext) {
        tradesDB.tradeExecutionQueries.lock(ids)
    }

    fun getById(id: TradeExecutionId): Flow<TradeExecution> {
        return tradesDB.tradeExecutionQueries
            .getById(id)
            .asFlow()
            .mapToOneOrNull(coroutineContext)
            .map { it ?: error("TradeExecution($id) not found") }
    }

    fun getTodayCount(): Flow<Long> {
        return tradesDB.tradeExecutionQueries.getTodayCount().asFlow().mapToOne(coroutineContext)
    }

    fun getBeforeTodayCount(): Flow<Long> {
        return tradesDB.tradeExecutionQueries.getBeforeTodayCount().asFlow().mapToOne(coroutineContext)
    }

    fun getAllDisplayPagingSource(): PagingSource<Int, TradeExecutionDisplay> = QueryPagingSource(
        countQuery = tradesDB.tradeExecutionQueries.getAllCount(),
        transacter = tradesDB.tradeExecutionQueries,
        context = coroutineContext,
        queryProvider = { limit, offset ->

            tradesDB.tradeExecutionDisplayQueries.getAllPaged(
                limit = limit,
                offset = offset,
            )
        },
    )

    fun getForTrade(id: TradeId): Flow<List<TradeExecution>> {
        return tradesDB.tradeToExecutionMapQueries
            .getExecutionsByTrade(id, ::toTradeExecution)
            .asFlow()
            .mapToList(coroutineContext)
    }

    fun getBySymbolInInterval(
        symbolId: SymbolId,
        range: ClosedRange<Instant>,
    ): Flow<List<TradeExecution>> {
        return tradesDB.tradeExecutionQueries
            .getBySymbolInInterval(
                symbolId = symbolId,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(coroutineContext)
    }

    fun getBySymbolAndTradeIdsInInterval(
        symbolId: SymbolId,
        ids: List<TradeId>,
        range: ClosedRange<Instant>,
    ): Flow<List<TradeExecution>> {
        return tradesDB.tradeToExecutionMapQueries
            .getExecutionsBySymbolAndTradeIdsInInterval(
                symbolId = symbolId,
                ids = ids,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
                mapper = ::toTradeExecution,
            )
            .asFlow()
            .mapToList(coroutineContext)
    }

    internal suspend fun deleteTrades(ids: List<TradeId>) = withContext(coroutineContext) {

        tradesDB.transaction {

            // Delete trades and executions
            tradesDB.tradeToExecutionMapQueries.deleteExecutionsAndTrades(ids)

            deleteSupplementalTradeData()
        }
    }

    private suspend fun isLocked(ids: List<TradeExecutionId>) = withContext(coroutineContext) {
        tradesDB.tradeExecutionQueries.isLocked(ids).executeAsList()
    }

    private fun consumeExecution(execution: TradeExecution) {

        // Currently open trades
        val openTrades = tradesDB.tradeQueries.getOpen().executeAsList()
        // Trade that will consume this execution
        val openTrade = openTrades.find {
            it.brokerId == execution.brokerId &&
                it.instrument == execution.instrument &&
                it.symbolId == execution.symbolId
        }

        // No open trade exists to consume execution. Create new trade.
        if (openTrade == null) {

            // Insert Trade
            val tradeId = tradesDB.tradeQueries.insert(
                brokerId = execution.brokerId,
                symbolId = execution.symbolId,
                instrument = execution.instrument,
                quantity = execution.quantity,
                closedQuantity = KBigDecimal.Zero,
                lots = execution.lots,
                closedLots = 0,
                side = if (execution.side == TradeExecutionSide.Buy) TradeSide.Long else TradeSide.Short,
                averageEntry = execution.price,
                entryTimestamp = execution.timestamp.withoutNanoseconds(),
                averageExit = null,
                exitTimestamp = null,
                pnl = KBigDecimal.Zero,
                fees = KBigDecimal.Zero,
                netPnl = KBigDecimal.Zero,
                isClosed = false,
            ).executeAsOne()

            // Link trade and execution in database
            tradesDB.tradeToExecutionMapQueries.insert(
                tradeId = tradeId,
                executionId = execution.id,
                overrideQuantity = null,
                overrideLots = null,
            )
        } else { // Open Trade exists. Update trade with new execution

            val isCloseAndOpenOrder = (openTrade.side == TradeSide.Long && execution.side == TradeExecutionSide.Sell) ||
                (openTrade.side == TradeSide.Short && execution.side == TradeExecutionSide.Buy)

            // Quantity of instrument that is still open after consuming current execution
            val currentOpenQuantity = openTrade.quantity - when {
                isCloseAndOpenOrder -> openTrade.closedQuantity + execution.quantity
                else -> openTrade.closedQuantity
            }

            // Lots of instrument that is still open after consuming current execution
            val currentOpenLots = openTrade.lots - when {
                isCloseAndOpenOrder -> openTrade.closedLots + execution.lots
                else -> openTrade.closedLots
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
            if (currentOpenQuantity < KBigDecimal.Zero) {

                // Link existing trade and execution in database, while overriding quantity
                tradesDB.tradeToExecutionMapQueries.insert(
                    tradeId = openTrade.id,
                    executionId = execution.id,
                    overrideQuantity = execution.quantity + currentOpenQuantity,
                    overrideLots = (execution.lots + currentOpenLots),
                )

                // Quantity for new trade
                val overrideQuantity = currentOpenQuantity.abs()
                val overrideLots = currentOpenLots.absoluteValue

                // Insert Trade
                val tradeId = tradesDB.tradeQueries.insert(
                    brokerId = execution.brokerId,
                    symbolId = execution.symbolId,
                    instrument = execution.instrument,
                    quantity = overrideQuantity,
                    closedQuantity = KBigDecimal.Zero,
                    lots = overrideLots,
                    closedLots = 0,
                    side = if (execution.side == TradeExecutionSide.Buy) TradeSide.Long else TradeSide.Short,
                    averageEntry = execution.price,
                    entryTimestamp = execution.timestamp.withoutNanoseconds(),
                    averageExit = null,
                    exitTimestamp = null,
                    pnl = KBigDecimal.Zero,
                    fees = KBigDecimal.Zero,
                    netPnl = KBigDecimal.Zero,
                    isClosed = false,
                ).executeAsOne()

                // Link new trade and current execution, override quantity with remainder quantity after previous trade
                // consumed some
                tradesDB.tradeToExecutionMapQueries.insert(
                    tradeId = tradeId,
                    executionId = execution.id,
                    overrideQuantity = overrideQuantity,
                    overrideLots = overrideLots,
                )
            } else {

                // Link trade and execution in database
                tradesDB.tradeToExecutionMapQueries.insert(
                    tradeId = openTrade.id,
                    executionId = execution.id,
                    overrideQuantity = null,
                    overrideLots = null,
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
        val entryLots = entryExecutions.sumOf { it.lots }
        val exitLots = exitExecutions.sumOf { it.lots }
        val averageEntry = entryExecutions.averagePrice()
        val averageExit = when {
            exitExecutions.isEmpty() -> null
            else -> {
                val extra = exitQuantity - entryQuantity
                when {
                    extra <= KBigDecimal.Zero -> exitExecutions.averagePrice()
                    else -> {
                        (
                            exitExecutions.dropLast(1) + exitExecutions.last()
                                .copy(quantity = exitExecutions.last().quantity - extra)
                        ).averagePrice()
                    }
                }
            }
        }
        val closedQuantity = minOf(exitQuantity, entryQuantity)
        val closedLots = minOf(exitLots, entryLots)

        val brokerage = averageExit?.let {

            val broker = brokerProvider.getBroker(firstExecution.brokerId)

            broker.calculateBrokerage(
                instrument = firstExecution.instrument,
                exchange = "NSE",
                entry = averageEntry,
                exit = averageExit,
                quantity = closedQuantity,
                isLong = side.isLong,
            )
        }

        return Trade(
            id = TradeId(-1),
            brokerId = firstExecution.brokerId,
            symbolId = firstExecution.symbolId,
            instrument = firstExecution.instrument,
            quantity = entryQuantity,
            closedQuantity = closedQuantity,
            lots = entryLots,
            closedLots = closedLots,
            side = side,
            averageEntry = averageEntry,
            entryTimestamp = firstExecution.timestamp.withoutNanoseconds(),
            averageExit = averageExit,
            exitTimestamp = exitExecutions.lastOrNull()?.timestamp?.withoutNanoseconds(),
            pnl = brokerage?.pnl ?: KBigDecimal.Zero,
            fees = brokerage?.totalCharges ?: KBigDecimal.Zero,
            netPnl = brokerage?.netPNL ?: KBigDecimal.Zero,
            isClosed = (exitQuantity - entryQuantity) >= KBigDecimal.Zero,
        )
    }

    private fun Trade.updateTradeInDB(tradeId: TradeId) {

        tradesDB.tradeQueries.update(
            id = tradeId,
            quantity = quantity,
            closedQuantity = closedQuantity,
            lots = lots,
            closedLots = closedLots,
            side = side,
            averageEntry = averageEntry,
            entryTimestamp = entryTimestamp.withoutNanoseconds(),
            averageExit = averageExit,
            exitTimestamp = exitTimestamp?.withoutNanoseconds(),
            pnl = pnl,
            fees = fees,
            netPnl = netPnl,
            isClosed = isClosed,
        )
    }

    private fun List<TradeExecution>.averagePrice(): KBigDecimal {

        val totalQuantity = sumOf { it.quantity }
        val sum = sumOf { it.price * it.quantity }

        return when (totalQuantity.compareTo(KBigDecimal.Zero)) {
            0 -> KBigDecimal.Zero
            else -> sum.div(totalQuantity, KMathContext.Decimal32)
        }
    }

    private fun toTradeExecution(
        id: TradeExecutionId,
        brokerId: BrokerId,
        symbolId: SymbolId,
        instrument: Instrument,
        quantity: KBigDecimal,
        lots: Int,
        side: TradeExecutionSide,
        price: KBigDecimal,
        timestamp: Instant,
        locked: Boolean,
        overrideQuantity: KBigDecimal?,
        overrideLots: Int?,
    ) = TradeExecution(
        id = id,
        brokerId = brokerId,
        instrument = instrument,
        symbolId = symbolId,
        quantity = overrideQuantity ?: quantity,
        lots = overrideLots ?: lots,
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
            if (attachmentsDir != null) {

                getOrphaned()
                    .executeAsList()
                    .forEach { attachmentsDir.resolve(it.fileName).deleteExisting() }
            }

            deleteOrphaned()
        }
    }

    private fun validateQuantity(
        quantity: KBigDecimal,
        symbol: Symbol,
    ) {

        val isValidQuantity = quantity.remainder(symbol.lotSize).compareTo(KBigDecimal.Zero) == 0

        require(isValidQuantity) { "Quantity is not valid. Quantity: $quantity, Lot Size: ${symbol.lotSize}." }
    }
}
