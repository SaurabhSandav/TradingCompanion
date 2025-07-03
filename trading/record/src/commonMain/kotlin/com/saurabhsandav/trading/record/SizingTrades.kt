package com.saurabhsandav.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.SizingTradeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

class SizingTrades(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
    private val brokerProvider: BrokerProvider,
) {

    suspend fun new(
        brokerId: BrokerId,
        symbolId: SymbolId,
        entry: BigDecimal,
        stop: BigDecimal,
    ) = withContext(coroutineContext) {

        tradesDB.transaction {

            // Add Broker
            tradesDB.brokerQueries.insert(
                id = brokerId,
                name = brokerProvider.getBroker(brokerId).name,
            )

            tradesDB.sizingTradeQueries.insert(
                brokerId = brokerId,
                symbolId = symbolId,
                entry = entry,
                stop = stop,
            )
        }
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
