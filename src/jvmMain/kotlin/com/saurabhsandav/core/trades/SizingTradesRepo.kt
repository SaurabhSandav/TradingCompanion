package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.trades.model.SizingTradeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class SizingTradesRepo(
    private val tradesDB: TradesDB,
) {

    suspend fun new(
        ticker: String,
        entry: BigDecimal,
        stop: BigDecimal,
    ) = withContext(Dispatchers.IO) {

        tradesDB.sizingTradeQueries.insert(
            ticker = ticker,
            entry = entry,
            stop = stop,
        )
    }

    suspend fun updateEntry(id: SizingTradeId, entry: BigDecimal) = withContext(Dispatchers.IO) {
        tradesDB.sizingTradeQueries.updateEntry(id = id, entry = entry)
    }

    suspend fun updateStop(id: SizingTradeId, stop: BigDecimal) = withContext(Dispatchers.IO) {
        tradesDB.sizingTradeQueries.updateStop(id = id, stop = stop)
    }

    suspend fun delete(id: SizingTradeId) = withContext(Dispatchers.IO) {
        tradesDB.sizingTradeQueries.delete(id)
    }

    val allTrades: Flow<List<SizingTrade>>
        get() = tradesDB.sizingTradeQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: SizingTradeId): Flow<SizingTrade> {
        return tradesDB.sizingTradeQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }
}
