package com.saurabhsandav.core.trades

import com.saurabhsandav.core.Trade
import com.saurabhsandav.core.TradeOrder
import com.saurabhsandav.core.TradesDB
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

internal class TradesRepo(
    private val tradesDB: TradesDB,
    private val tradeOrdersRepo: TradeOrdersRepo,
) {

    val allTrades: Flow<List<Trade>>
        get() = tradesDB.tradeQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<Trade> {
        return tradesDB.tradeQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getOrdersForTrade(id: Long): Flow<List<TradeOrder>> {
        return tradeOrdersRepo.getOrdersForTrade(id)
    }
}
