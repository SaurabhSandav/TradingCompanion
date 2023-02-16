package com.saurabhsandav.core.trades

import com.saurabhsandav.core.*
import com.saurabhsandav.core.trades.model.TradeSide
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class TradesRepo(
    private val tradesDB: TradesDB,
    private val tradeOrdersRepo: TradeOrdersRepo,
) {

    val allTrades: Flow<List<Trade>>
        get() = tradesDB.tradeQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<Trade> {
        return tradesDB.tradeQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getOrdersForTrade(id: Long): Flow<List<TradeOrder>> {
        return tradeOrdersRepo.getOrdersForTrade(id)
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
}
