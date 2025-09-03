package com.saurabhsandav.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.broker.BrokerId
import com.saurabhsandav.trading.broker.BrokerProvider
import com.saurabhsandav.trading.core.SymbolId
import com.saurabhsandav.trading.record.model.SizingTradeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class SizingTrades(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
    private val brokerProvider: BrokerProvider,
    private val getSymbol: (suspend (BrokerId, SymbolId) -> Symbol?)?,
) {

    suspend fun new(
        brokerId: BrokerId,
        symbolId: SymbolId,
        entry: KBigDecimal,
        stop: KBigDecimal,
    ) = withContext(coroutineContext) {

        val broker = brokerProvider.getBroker(brokerId)
        val symbol = getSymbol?.invoke(brokerId, symbolId)

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
        entry: KBigDecimal,
    ) = withContext(coroutineContext) {
        tradesDB.sizingTradeQueries.updateEntry(id = id, entry = entry)
    }

    suspend fun updateStop(
        id: SizingTradeId,
        stop: KBigDecimal,
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
