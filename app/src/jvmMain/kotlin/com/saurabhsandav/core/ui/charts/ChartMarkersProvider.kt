package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.trading.TradingProfiles
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.ui.tradecontent.ProfileTradeId
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.mapList
import com.saurabhsandav.trading.core.SymbolId
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Instant

@SingleIn(ChartsGraph::class)
@Inject
internal class ChartMarkersProvider(
    private val appDispatchers: AppDispatchers,
    private val tradingProfiles: TradingProfiles,
) {

    private val markedTradeIds = MutableStateFlow<List<ProfileTradeId>>(emptyList())

    fun getTradeMarkers(
        symbolId: SymbolId,
        instantRange: ClosedRange<Instant>,
    ): Flow<List<TradeMarker>> {

        return markedTradeIds.flatMapLatest { profileTradeIds ->

            val tradeIdsByProfileIds = profileTradeIds.groupBy(
                keySelector = { it.profileId },
                valueTransform = { it.tradeId },
            )

            tradeIdsByProfileIds
                .map { (profileId, tradeIds) ->

                    val tradingRecord = tradingProfiles.getRecord(profileId)

                    tradingRecord.trades
                        .getBySymbolAndIdsInInterval(symbolId, tradeIds, instantRange)
                        .flatMapLatest { markedTrades ->

                            val closedTrades = markedTrades.filter { it.isClosed }
                            val closedTradesIds = closedTrades.map { it.id }

                            combine(
                                tradingRecord.stops.getPrimary(closedTradesIds),
                                tradingRecord.targets.getPrimary(closedTradesIds),
                            ) { stops, targets ->

                                closedTrades.mapNotNull { trade ->

                                    val stop = stops.find { it.tradeId == trade.id } ?: return@mapNotNull null
                                    val target = targets.find { it.tradeId == trade.id } ?: return@mapNotNull null

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
                }
                .let { flows ->
                    when {
                        flows.isEmpty() -> flowOf(emptyList())
                        else -> combine(flows) { it.toList().flatten() }
                    }
                }
        }.flowOn(appDispatchers.IO)
    }

    fun getTradeExecutionMarkers(
        symbolId: SymbolId,
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

                    tradingRecord.executions.getBySymbolAndTradeIdsInInterval(
                        symbolId = symbolId,
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
        }.flowOn(appDispatchers.IO)
    }

    fun setMarkedTrades(ids: List<ProfileTradeId>) {
        markedTradeIds.value = ids.toList()
    }
}
