package com.saurabhsandav.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.trading.record.model.SizingTradeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

class SizingTrades(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
) {

    suspend fun new(
        ticker: String,
        entry: BigDecimal,
        stop: BigDecimal,
    ) = withContext(coroutineContext) {

        tradesDB.sizingTradeQueries.insert(
            ticker = ticker,
            entry = entry,
            stop = stop,
        )
    }

    suspend fun updateEntry(
        id: SizingTradeId,
        entry: BigDecimal,
    ) = withContext(coroutineContext) {
        tradesDB.sizingTradeQueries.updateEntry(id = id, entry = entry)
    }

    suspend fun updateStop(
        id: SizingTradeId,
        stop: BigDecimal,
    ) = withContext(coroutineContext) {
        tradesDB.sizingTradeQueries.updateStop(id = id, stop = stop)
    }

    suspend fun delete(id: SizingTradeId) = withContext(coroutineContext) {
        tradesDB.sizingTradeQueries.delete(id)
    }

    val allTrades: Flow<List<SizingTrade>>
        get() = tradesDB.sizingTradeQueries.getAll().asFlow().mapToList(coroutineContext)

    fun getById(id: SizingTradeId): Flow<SizingTrade> {
        return tradesDB.sizingTradeQueries.getById(id).asFlow().mapToOne(coroutineContext)
    }
}
