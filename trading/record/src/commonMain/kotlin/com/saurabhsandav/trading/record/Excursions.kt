package com.saurabhsandav.trading.record

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.kbigdecimal.KBigDecimal
import com.saurabhsandav.trading.record.model.TradeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class Excursions internal constructor(
    private val coroutineContext: CoroutineContext,
    private val tradesDB: TradesDB,
) {

    suspend fun set(
        id: TradeId,
        tradeMfePrice: KBigDecimal,
        tradeMfePnl: KBigDecimal,
        tradeMaePrice: KBigDecimal,
        tradeMaePnl: KBigDecimal,
        sessionMfePrice: KBigDecimal,
        sessionMfePnl: KBigDecimal,
        sessionMaePrice: KBigDecimal,
        sessionMaePnl: KBigDecimal,
    ) = withContext(coroutineContext) {

        // Save Excursions
        tradesDB.tradeExcursionsQueries.insert(
            tradeId = id,
            tradeMfePrice = tradeMfePrice,
            tradeMfePnl = tradeMfePnl,
            tradeMaePrice = tradeMaePrice,
            tradeMaePnl = tradeMaePnl,
            sessionMfePrice = sessionMfePrice,
            sessionMfePnl = sessionMfePnl,
            sessionMaePrice = sessionMaePrice,
            sessionMaePnl = sessionMaePnl,
        )
    }

    fun get(id: TradeId): Flow<TradeExcursions?> {
        return tradesDB.tradeExcursionsQueries.getByTrade(id).asFlow().mapToOneOrNull(coroutineContext)
    }
}
