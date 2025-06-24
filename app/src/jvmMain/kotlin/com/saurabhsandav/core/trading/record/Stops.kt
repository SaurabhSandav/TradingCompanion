package com.saurabhsandav.core.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.trading.record.model.TradeId
import com.saurabhsandav.core.trading.record.model.TradeSide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

class Stops internal constructor(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
) {

    fun getForTrade(id: TradeId): Flow<List<TradeStop>> {
        return tradesDB.tradeStopQueries.getByTrade(id).asFlow().mapToList(coroutineContext)
    }

    fun getPrimary(id: TradeId): Flow<TradeStop?> {
        return tradesDB.tradeStopQueries.getPrimaryStopByTrade(id).asFlow().mapToOneOrNull(coroutineContext)
    }

    fun getPrimary(ids: List<TradeId>): Flow<List<TradeStop>> {
        return tradesDB.tradeStopQueries.getPrimaryStopsByTrades(ids).asFlow().mapToList(coroutineContext)
    }

    suspend fun add(
        id: TradeId,
        price: BigDecimal,
    ) = withContext(coroutineContext) {

        val trade = tradesDB.tradeQueries.getById(id).executeAsOne()

        val stopIsValid = when (trade.side) {
            TradeSide.Long -> price < trade.averageEntry
            TradeSide.Short -> price > trade.averageEntry
        }

        if (!stopIsValid) error("Invalid stop for Trade (#$id)")

        // Insert into DB
        tradesDB.tradeStopQueries.insert(
            tradeId = id,
            price = price.stripTrailingZeros(),
        )

        // Delete Excursions. Excursions use primary stop to generate session mfe/mae.
        tradesDB.tradeExcursionsQueries.delete(id)
    }

    suspend fun delete(
        id: TradeId,
        price: BigDecimal,
    ) = withContext(coroutineContext) {

        // Delete stop
        tradesDB.tradeStopQueries.delete(tradeId = id, price = price)

        // Delete Excursions. Excursions use primary stop to generate session mfe/mae.
        tradesDB.tradeExcursionsQueries.delete(id)
    }

    suspend fun setPrimary(
        id: TradeId,
        price: BigDecimal,
    ) = withContext(coroutineContext) {

        tradesDB.tradeStopQueries.setPrimary(tradeId = id, price = price)
    }
}
