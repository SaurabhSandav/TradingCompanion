package com.saurabhsandav.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.trading.record.model.TradeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

class Excursions internal constructor(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
) {

    suspend fun set(
        id: TradeId,
        tradeMfePrice: BigDecimal,
        tradeMfePnl: BigDecimal,
        tradeMaePrice: BigDecimal,
        tradeMaePnl: BigDecimal,
        sessionMfePrice: BigDecimal,
        sessionMfePnl: BigDecimal,
        sessionMaePrice: BigDecimal,
        sessionMaePnl: BigDecimal,
    ) = withContext(coroutineContext) {

        // Save Excursions
        tradesDB.tradeExcursionsQueries.insert(
            tradeId = id,
            tradeMfePrice = tradeMfePrice.stripTrailingZeros(),
            tradeMfePnl = tradeMfePnl.stripTrailingZeros(),
            tradeMaePrice = tradeMaePrice.stripTrailingZeros(),
            tradeMaePnl = tradeMaePnl.stripTrailingZeros(),
            sessionMfePrice = sessionMfePrice.stripTrailingZeros(),
            sessionMfePnl = sessionMfePnl.stripTrailingZeros(),
            sessionMaePrice = sessionMaePrice.stripTrailingZeros(),
            sessionMaePnl = sessionMaePnl.stripTrailingZeros(),
        )
    }

    fun get(id: TradeId): Flow<TradeExcursions?> {
        return tradesDB.tradeExcursionsQueries.getByTrade(id).asFlow().mapToOneOrNull(coroutineContext)
    }
}
