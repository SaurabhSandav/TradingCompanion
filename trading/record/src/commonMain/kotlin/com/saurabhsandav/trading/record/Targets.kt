package com.saurabhsandav.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.trading.record.model.TradeId
import com.saurabhsandav.trading.record.model.TradeSide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

class Targets internal constructor(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
) {

    fun getForTrade(id: TradeId): Flow<List<TradeTarget>> {
        return tradesDB.tradeTargetQueries.getByTrade(id).asFlow().mapToList(coroutineContext)
    }

    fun getPrimary(id: TradeId): Flow<TradeTarget?> {
        return tradesDB.tradeTargetQueries.getPrimaryTargetByTrade(id).asFlow().mapToOneOrNull(coroutineContext)
    }

    fun getPrimary(ids: List<TradeId>): Flow<List<TradeTarget>> {
        return tradesDB.tradeTargetQueries.getPrimaryTargetsByTrades(ids).asFlow().mapToList(coroutineContext)
    }

    suspend fun add(
        id: TradeId,
        price: BigDecimal,
    ) = withContext(coroutineContext) {

        val trade = tradesDB.tradeQueries.getById(id).executeAsOne()

        val targetIsValid = when (trade.side) {
            TradeSide.Long -> price > trade.averageEntry
            TradeSide.Short -> price < trade.averageEntry
        }

        if (!targetIsValid) error("Invalid target for Trade (#$id)")

        // Insert into DB
        tradesDB.tradeTargetQueries.insert(
            tradeId = id,
            price = price.stripTrailingZeros(),
        )

        // Delete Excursions. Excursions use primary target to generate session mfe/mae.
        tradesDB.tradeExcursionsQueries.delete(id)
    }

    suspend fun delete(
        id: TradeId,
        price: BigDecimal,
    ) = withContext(coroutineContext) {

        // Delete target
        tradesDB.tradeTargetQueries.delete(tradeId = id, price = price)

        // Delete Excursions. Excursions use primary target to generate session mfe/mae.
        tradesDB.tradeExcursionsQueries.delete(id)
    }

    suspend fun setPrimary(
        id: TradeId,
        price: BigDecimal,
    ) = withContext(coroutineContext) {

        tradesDB.tradeTargetQueries.setPrimary(tradeId = id, price = price)
    }
}
