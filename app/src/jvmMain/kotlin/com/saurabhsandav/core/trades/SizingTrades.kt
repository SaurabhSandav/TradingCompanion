package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.core.trades.model.SizingTradeId
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class SizingTrades(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
) {

    suspend fun new(
        ticker: String,
        entry: BigDecimal,
        stop: BigDecimal,
    ) = withContext(appDispatchers.IO) {

        tradesDB.sizingTradeQueries.insert(
            ticker = ticker,
            entry = entry,
            stop = stop,
        )
    }

    suspend fun updateEntry(id: SizingTradeId, entry: BigDecimal) = withContext(appDispatchers.IO) {
        tradesDB.sizingTradeQueries.updateEntry(id = id, entry = entry)
    }

    suspend fun updateStop(id: SizingTradeId, stop: BigDecimal) = withContext(appDispatchers.IO) {
        tradesDB.sizingTradeQueries.updateStop(id = id, stop = stop)
    }

    suspend fun delete(id: SizingTradeId) = withContext(appDispatchers.IO) {
        tradesDB.sizingTradeQueries.delete(id)
    }

    val allTrades: Flow<List<SizingTrade>>
        get() = tradesDB.sizingTradeQueries.getAll().asFlow().mapToList(appDispatchers.IO)

    fun getById(id: SizingTradeId): Flow<SizingTrade> {
        return tradesDB.sizingTradeQueries.getById(id).asFlow().mapToOne(appDispatchers.IO)
    }
}
