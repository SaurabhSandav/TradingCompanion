package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.github.michaelbull.result.get
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.data.CandleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes

internal class TradesRepo(
    private val tradesDB: TradesDB,
    private val tradeOrdersRepo: TradeOrdersRepo,
    private val candleRepo: CandleRepository,
) {

    val allTrades: Flow<List<Trade>>
        get() = tradesDB.tradeQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<Trade> {
        return tradesDB.tradeQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getByTickerInInterval(
        ticker: String,
        range: ClosedRange<LocalDateTime>,
    ): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getByTickerInInterval(
                ticker = ticker,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getByTickerAndIdsInInterval(
        ticker: String,
        ids: List<Long>,
        range: ClosedRange<LocalDateTime>,
    ): Flow<List<Trade>> {
        return tradesDB.tradeQueries
            .getByTickerAndIdsInInterval(
                ticker = ticker,
                ids = ids,
                from = range.start.toString(),
                to = range.endInclusive.toString(),
            )
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getOrdersForTrade(id: Long): Flow<List<TradeOrder>> {
        return tradeOrdersRepo.getOrdersForTrade(id)
    }

    fun getTradesForOrder(orderId: Long): Flow<List<Trade>> {
        return tradesDB.tradeToExecutionMapQueries
            .getTradesByOrder(orderId)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    suspend fun generateMfeAndMaeForAllTrades() = withContext(Dispatchers.IO) {

        val trades = tradesDB.tradeMfeMaeQueries.getIfMfeAndMaeNotGenerated().executeAsList()

        trades.forEach { trade ->

            // If candle is not closed, skip calculation
            if (!trade.isClosed) return@forEach

            val entryInstant = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())
            val exitInstant = trade.exitTimestamp!!.toInstant(TimeZone.currentSystemDefault())

            // Get candles for trade. Get a few extra at the beginning, so that we don't miss any candles.
            val candles = candleRepo.getCandles(
                ticker = trade.ticker,
                timeframe = Timeframe.M1,
                from = entryInstant - 2.minutes,
                to = exitInstant,
            ).get() ?: return@forEach

            // Candles to consider while calculating MFE and MAE
            val tradeCandles = candles.filter { candle ->
                // Check if candle inside trade interval OR
                // Check if entry time inside candle interval
                candle.openInstant in entryInstant..exitInstant ||
                        entryInstant in candle.openInstant..(candle.openInstant + 1.minutes)
            }

            // Save MFE and MAE
            tradesDB.tradeMfeMaeQueries.insert(
                tradeId = trade.id,
                mfePrice = when (trade.side) {
                    TradeSide.Long -> tradeCandles.maxOf { it.high }
                    TradeSide.Short -> tradeCandles.minOf { it.low }
                },
                maePrice = when (trade.side) {
                    TradeSide.Long -> tradeCandles.minOf { it.low }
                    TradeSide.Short -> tradeCandles.maxOf { it.high }
                },
            )

            // This logic does not need to run uninterrupted. Let other coroutines a chance to run.
            yield()
        }
    }

    fun getMfeAndMae(id: Long): Flow<TradeMfeMae?> {
        return tradesDB.tradeMfeMaeQueries.getByTrade(id).asFlow().mapToOneOrNull(Dispatchers.IO)
    }

    fun getStopsForTrade(id: Long): Flow<List<TradeStop>> {
        return tradesDB.tradeStopQueries.getByTrade(id).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun addStop(id: Long, price: BigDecimal) = withContext(Dispatchers.IO) {

        // Get trade
        val trade = tradesDB.tradeQueries.getById(id).executeAsOne()

        // Calculate risk
        val risk = when (trade.side) {
            TradeSide.Long -> trade.averageEntry - price
            TradeSide.Short -> price - trade.averageEntry
        } * trade.quantity

        // Insert into DB
        tradesDB.tradeStopQueries.insert(
            tradeId = id,
            price = price,
            risk = risk,
        )
    }

    suspend fun deleteStop(id: Long, price: BigDecimal) = withContext(Dispatchers.IO) {
        tradesDB.tradeStopQueries.delete(tradeId = id, price = price)
    }

    fun getTargetsForTrade(id: Long): Flow<List<TradeTarget>> {
        return tradesDB.tradeTargetQueries.getByTrade(id).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun addTarget(id: Long, price: BigDecimal) = withContext(Dispatchers.IO) {

        // Get trade
        val trade = tradesDB.tradeQueries.getById(id).executeAsOne()

        // Calculate profit
        val profit = when (trade.side) {
            TradeSide.Long -> price - trade.averageEntry
            TradeSide.Short -> trade.averageEntry - price
        } * trade.quantity

        // Insert into DB
        tradesDB.tradeTargetQueries.insert(
            tradeId = id,
            price = price,
            profit = profit,
        )
    }

    suspend fun deleteTarget(id: Long, price: BigDecimal) = withContext(Dispatchers.IO) {
        tradesDB.tradeTargetQueries.delete(tradeId = id, price = price)
    }

    fun getNotesForTrade(id: Long): Flow<List<TradeNote>> {
        return tradesDB.tradeNoteQueries.getByTrade(id).asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun addNote(tradeId: Long, note: String) = withContext(Dispatchers.IO) {

        val now = Clock.System.now()

        tradesDB.tradeNoteQueries.insert(
            tradeId = tradeId,
            note = note,
            added = now,
            lastEdited = now,
        )
    }

    suspend fun updateNote(id: Long, note: String) = withContext(Dispatchers.IO) {

        tradesDB.tradeNoteQueries.update(
            id = id,
            note = note,
            lastEdited = Clock.System.now(),
        )
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        tradesDB.tradeNoteQueries.delete(id)
    }
}
