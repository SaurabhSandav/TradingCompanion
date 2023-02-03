package com.saurabhsandav.core.trades

import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.trades.model.Trade
import com.saurabhsandav.core.trades.model.TradeOrder
import com.saurabhsandav.core.trades.model.TradeSide
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOne
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

internal class TradesRepo(
    private val appDB: AppDB,
    private val tradeOrdersRepo: TradeOrdersRepo,
) {

    val allTrades: Flow<List<Trade>>
        get() = appDB.tradeQueries.getAll(::dbTradeToTradeMapper).asFlow().mapToList(Dispatchers.IO)

    fun getById(id: Long): Flow<Trade> {
        return appDB.tradeQueries.getById(id, ::dbTradeToTradeMapper).asFlow().mapToOne(Dispatchers.IO)
    }

    fun getOrdersForTrade(id: Long): Flow<List<TradeOrder>> {
        return tradeOrdersRepo.getOrdersForTrade(id)
    }
}

internal fun dbTradeToTradeMapper(
    id: Long,
    broker: String,
    ticker: String,
    instrument: String,
    quantity: String,
    closedQuantity: String,
    lots: Int?,
    side: String,
    averageEntry: String,
    entryTimestamp: String,
    averageExit: String?,
    exitTimestamp: String?,
    pnl: String,
    fees: String,
    netPnl: String,
    isClosed: String,
): Trade = Trade(
    id = id,
    broker = broker,
    ticker = ticker,
    instrument = instrument,
    quantity = quantity.toInt(),
    closedQuantity = closedQuantity.toInt(),
    lots = lots,
    side = TradeSide.fromString(side),
    averageEntry = averageEntry.toBigDecimal(),
    entryTimestamp = LocalDateTime.parse(entryTimestamp),
    averageExit = averageExit?.toBigDecimal(),
    exitTimestamp = exitTimestamp?.let { LocalDateTime.parse(it) },
    pnl = pnl.toBigDecimal(),
    fees = fees.toBigDecimal(),
    netPnl = netPnl.toBigDecimal(),
    isClosed = isClosed.toBoolean(),
)
