package com.saurabhsandav.core.utils

import com.saurabhsandav.core.AppDB
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trades.model.TradeOrder
import com.saurabhsandav.core.trades.model.TradeSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime

internal class TradeMigrator(
    private val tradeOrdersRepo: TradeOrdersRepo,
    private val appDB: AppDB,
) {

    fun migrateTrades() = CoroutineScope(Dispatchers.IO).launchUnit {

        val migratedTradeOrders = getClosedTradesAsTradeOrders()

        migratedTradeOrders.forEach {
            tradeOrdersRepo.new(
                broker = it.broker,
                ticker = it.ticker,
                quantity = it.quantity,
                lots = it.lots,
                type = it.type,
                price = it.price,
                timestamp = it.timestamp,
            )
        }
    }

    private fun getClosedTradesAsTradeOrders(): List<TradeOrder> {

        val closedTrades = appDB.closedTradeQueries.getAll().executeAsList()

        val orders = closedTrades.flatMap {

            when (TradeSide.fromString(it.side)) {
                TradeSide.Long -> {
                    listOf(
                        TradeOrder(
                            id = 0,
                            broker = it.broker,
                            ticker = it.ticker,
                            quantity = it.quantity.toInt(),
                            lots = it.lots,
                            type = OrderType.Buy,
                            price = it.entry.toBigDecimal(),
                            timestamp = LocalDateTime.parse(it.entryDate),
                        ),
                        TradeOrder(
                            id = 0,
                            broker = it.broker,
                            ticker = it.ticker,
                            quantity = it.quantity.toInt(),
                            lots = it.lots,
                            type = OrderType.Sell,
                            price = it.exit.toBigDecimal(),
                            timestamp = LocalDateTime.parse(it.exitDate),
                        )
                    )
                }

                TradeSide.Short -> {
                    listOf(
                        TradeOrder(
                            id = 0,
                            broker = it.broker,
                            ticker = it.ticker,
                            quantity = it.quantity.toInt(),
                            lots = it.lots,
                            type = OrderType.Sell,
                            price = it.entry.toBigDecimal(),
                            timestamp = LocalDateTime.parse(it.entryDate),
                        ),
                        TradeOrder(
                            id = 0,
                            broker = it.broker,
                            ticker = it.ticker,
                            quantity = it.quantity.toInt(),
                            lots = it.lots,
                            type = OrderType.Buy,
                            price = it.exit.toBigDecimal(),
                            timestamp = LocalDateTime.parse(it.exitDate),
                        )
                    )
                }
            }
        }.mergeSameOrders().sortedBy { it.timestamp }

        return orders
    }

    private fun List<TradeOrder>.mergeSameOrders(): List<TradeOrder> {

        val allMergeOrders = mutableListOf<List<TradeOrder>>()

        forEachIndexed { index, first ->

            val mergeOrders = subListInclusive(index + 1, lastIndex).mapNotNull { second ->

                when {
                    first.broker == second.broker && first.ticker == second.ticker && first.type == second.type
                            && first.price == second.price && first.timestamp == second.timestamp -> second

                    else -> null
                }
            }

            if (mergeOrders.isNotEmpty()) {
                allMergeOrders.add(listOf(first) + mergeOrders)
            }
        }

        val mutableThis = toMutableList()

        allMergeOrders.mapTo(mutableThis) { mergeOrders ->

            mutableThis.removeAll(mergeOrders)

            val lots = mergeOrders.mapNotNull { it.lots }.sumOf { it }

            mergeOrders.first().copy(
                quantity = mergeOrders.sumOf { it.quantity },
                lots = if (lots == 0) null else lots,
            )
        }

        return mutableThis
    }
}
