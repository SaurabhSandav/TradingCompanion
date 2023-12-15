package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.Stable
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant

@Stable
internal class ChartMarkersProvider(
    private val tradingProfiles: TradingProfiles,
) {

    private val markedTradeIds = MutableStateFlow<List<ProfileTradeId>>(emptyList())

    fun getTradeMarkers(ticker: String, instantRange: ClosedRange<Instant>): Flow<List<TradeMarker>> {

        return markedTradeIds.flatMapLatest { profileTradeIds ->

            val tradeIdsByProfileIds = profileTradeIds.groupBy(
                keySelector = { it.profileId },
                valueTransform = { it.tradeId },
            )

            tradeIdsByProfileIds
                .map { (profileId, tradeIds) ->

                    val tradingRecord = tradingProfiles.getRecord(profileId)

                    tradingRecord.trades
                        .getByTickerAndIdsInInterval(ticker, tradeIds, instantRange)
                        .map { trades ->

                            trades.filter { it.isClosed }.mapNotNull { trade ->

                                val stop = tradingRecord
                                    .trades
                                    .getStopsForTrade(trade.id)
                                    .first()
                                    .maxByOrNull { (trade.averageEntry - it.price).abs() }
                                    ?: return@mapNotNull null

                                val target = tradingRecord
                                    .trades
                                    .getTargetsForTrade(trade.id)
                                    .first()
                                    .maxByOrNull { (trade.averageEntry - it.price).abs() }
                                    ?: return@mapNotNull null

                                TradeMarker(
                                    entryPrice = trade.averageEntry,
                                    exitPrice = trade.averageExit!!,
                                    entryInstant = trade.entryTimestamp,
                                    exitInstant = trade.exitTimestamp!!,
                                    stopPrice = stop.price,
                                    targetPrice = target.price,
                                )
                            }
                        }
                }
                .let { flows ->
                    when {
                        flows.isEmpty() -> flowOf(emptyList())
                        else -> combine(flows) { it.toList().flatten() }
                    }
                }
        }.flowOn(Dispatchers.IO)
    }

    fun getTradeExecutionMarkers(
        ticker: String,
        instantRange: ClosedRange<Instant>,
    ): Flow<List<TradeExecutionMarker>> {

        return markedTradeIds.flatMapLatest { profileTradeIds ->

            val tradeIdsByProfileIds = profileTradeIds.groupBy(
                keySelector = { it.profileId },
                valueTransform = { it.tradeId },
            )

            tradeIdsByProfileIds
                .map { (profileId, tradeIds) ->

                    val tradingRecord = tradingProfiles.getRecord(profileId)

                    tradingRecord.executions.getExecutionsByTickerAndTradeIdsInInterval(
                        ticker = ticker,
                        ids = tradeIds,
                        range = instantRange,
                    )
                }
                .let { flows ->
                    when {
                        flows.isEmpty() -> flowOf(emptyList())
                        else -> combine(flows) { it.toList().flatten() }
                    }
                }
                .mapList { execution ->

                    TradeExecutionMarker(
                        instant = execution.timestamp,
                        side = execution.side,
                        price = execution.price,
                    )
                }
        }.flowOn(Dispatchers.IO)
    }

    fun setMarkedTrades(ids: List<ProfileTradeId>) {
        markedTradeIds.value = ids.toList()
    }
}
