package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trades.model.TradeSide
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class Targets internal constructor(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
) {

    fun getForTrade(id: TradeId): Flow<List<TradeTarget>> {
        return tradesDB.tradeTargetQueries.getByTrade(id).asFlow().mapToList(appDispatchers.IO)
    }

    fun getPrimary(id: TradeId): Flow<TradeTarget?> {
        return tradesDB.tradeTargetQueries.getPrimaryTargetByTrade(id).asFlow().mapToOneOrNull(appDispatchers.IO)
    }

    fun getPrimary(ids: List<TradeId>): Flow<List<TradeTarget>> {
        return tradesDB.tradeTargetQueries.getPrimaryTargetsByTrades(ids).asFlow().mapToList(appDispatchers.IO)
    }

    suspend fun add(
        id: TradeId,
        price: BigDecimal,
    ) = withContext(appDispatchers.IO) {

        val trade = tradesDB.tradeQueries.getById(id).asFlow().mapToOne(appDispatchers.IO).first()

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
    ) = withContext(appDispatchers.IO) {

        // Delete target
        tradesDB.tradeTargetQueries.delete(tradeId = id, price = price)

        // Delete Excursions. Excursions use primary target to generate session mfe/mae.
        tradesDB.tradeExcursionsQueries.delete(id)
    }

    suspend fun setPrimary(
        id: TradeId,
        price: BigDecimal,
    ) = withContext(appDispatchers.IO) {

        tradesDB.tradeTargetQueries.setPrimary(tradeId = id, price = price)
    }
}
