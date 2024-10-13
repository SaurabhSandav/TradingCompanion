package com.saurabhsandav.core.trades

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.utils.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class Excursions internal constructor(
    private val appDispatchers: AppDispatchers,
    private val tradesDB: TradesDB,
) {

    suspend fun setExcursions(
        id: TradeId,
        tradeMfePrice: BigDecimal,
        tradeMfePnl: BigDecimal,
        tradeMaePrice: BigDecimal,
        tradeMaePnl: BigDecimal,
        sessionMfePrice: BigDecimal,
        sessionMfePnl: BigDecimal,
        sessionMaePrice: BigDecimal,
        sessionMaePnl: BigDecimal,
    ) = withContext(appDispatchers.IO) {

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

    fun getExcursions(id: TradeId): Flow<TradeExcursions?> {
        return tradesDB.tradeExcursionsQueries.getByTrade(id).asFlow().mapToOneOrNull(appDispatchers.IO)
    }
}
