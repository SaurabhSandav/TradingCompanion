package com.saurabhsandav.core.trades

import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.Trade
import com.saurabhsandav.core.TradeOrder
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

internal class TradesRepo(
    private val appDB: AppDB,
    private val tradeOrdersRepo: TradeOrdersRepo,
) {

    val allTrades: Flow<List<Trade>>
        get() = appDB.tradeQueries.getAll().asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<Trade> {
        return appDB.tradeQueries.getById(id).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getOrdersForTrade(id: Long): Flow<List<TradeOrder>> {
        return tradeOrdersRepo.getOrdersForTrade(id)
    }
}
