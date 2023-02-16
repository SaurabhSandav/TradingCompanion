package com.saurabhsandav.core.utils

import com.saurabhsandav.core.*
import com.saurabhsandav.core.trades.TradeOrdersRepo
import com.saurabhsandav.core.trades.TradesRepo
import com.saurabhsandav.core.trades.model.OrderType
import com.saurabhsandav.core.trades.model.TradeSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime

internal class TradeMigrator(
    private val tradesRepo: TradesRepo,
    private val tradeOrdersRepo: TradeOrdersRepo,
    private val appDB: AppDB,
    private val tradesDB: TradesDB,
) {

    fun migrateTrades() = CoroutineScope(Dispatchers.IO).launchUnit {

        println("Synthesizing orders")

        // Synthesize orders from ClosedTrades
        val migratedTradeOrders = getClosedTradesAsTradeOrders()

        println("Generating trades")

        // Save orders and generate trades
        migratedTradeOrders.forEach {
            tradeOrdersRepo.new(
                broker = it.broker,
                ticker = it.ticker,
                quantity = it.quantity,
                lots = it.lots,
                type = it.type,
                price = it.price,
                timestamp = it.timestamp,
                locked = it.locked,
            )
        }

        println("Associating new Trades with ClosedTrades")

        val tradesWithClosedTrades = associateTradesWithClosedTrades()

        println("Migrating Stops")

        // Migrate Stops
        tradesWithClosedTrades.migrateStops()

        println("Migrating Targets")

        // Migrate Targets
        tradesWithClosedTrades.migrateTargets()

        println("Done")
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
                            quantity = it.quantity.toBigDecimal(),
                            lots = it.lots,
                            type = OrderType.Buy,
                            price = it.entry.toBigDecimal(),
                            timestamp = LocalDateTime.parse(it.entryDate),
                            locked = true,
                        ),
                        TradeOrder(
                            id = 0,
                            broker = it.broker,
                            ticker = it.ticker,
                            quantity = it.quantity.toBigDecimal(),
                            lots = it.lots,
                            type = OrderType.Sell,
                            price = it.exit.toBigDecimal(),
                            timestamp = LocalDateTime.parse(it.exitDate),
                            locked = true,
                        )
                    )
                }

                TradeSide.Short -> {
                    listOf(
                        TradeOrder(
                            id = 0,
                            broker = it.broker,
                            ticker = it.ticker,
                            quantity = it.quantity.toBigDecimal(),
                            lots = it.lots,
                            type = OrderType.Sell,
                            price = it.entry.toBigDecimal(),
                            timestamp = LocalDateTime.parse(it.entryDate),
                            locked = true,
                        ),
                        TradeOrder(
                            id = 0,
                            broker = it.broker,
                            ticker = it.ticker,
                            quantity = it.quantity.toBigDecimal(),
                            lots = it.lots,
                            type = OrderType.Buy,
                            price = it.exit.toBigDecimal(),
                            timestamp = LocalDateTime.parse(it.exitDate),
                            locked = true,
                        )
                    )
                }
            }
        }.mergeSameOrders().sortedBy { it.timestamp }

        return orders
    }

    private fun associateTradesWithClosedTrades(): Map<Trade, List<ClosedTrade>> {

        val closedTrades = appDB.closedTradeQueries.getAll().executeAsList()
        val trades = tradesDB.tradeQueries.getAll().executeAsList()

        return trades.sortedBy { it.entryTimestamp }.associateWith { trade ->
            closedTrades.filter {
                trade.ticker == it.ticker && LocalDateTime.parse(it.entryDate) >= trade.entryTimestamp
                        && LocalDateTime.parse(it.exitDate) <= trade.exitTimestamp!!
            }
        }
    }

    private suspend fun Map<Trade, List<ClosedTrade>>.migrateStops() {
        forEach { (trade, closedTrades) ->
            closedTrades
                .mapNotNull { if (it.stop.isNullOrBlank()) null else it.stop }
                .forEach { tradesRepo.addStop(trade.id, it.toBigDecimal()) }
        }
    }

    private suspend fun Map<Trade, List<ClosedTrade>>.migrateTargets() {
        forEach { (trade, closedTrades) ->
            closedTrades
                .mapNotNull { if (it.target.isNullOrBlank()) null else it.target }
                .forEach { tradesRepo.addTarget(trade.id, it.toBigDecimal()) }
        }
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
