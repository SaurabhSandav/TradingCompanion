package com.saurabhsandav.core.ui.charts

import androidx.compose.runtime.Stable
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trades.model.TradeId
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant

@Stable
internal class ChartMarkersProvider(
    private val appModule: AppModule,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
    private val appPrefs: FlowSettings = appModule.appPrefs,
) {

    val markedTradeIds = MutableStateFlow<Set<TradeId>>(persistentSetOf())

    fun getTradeMarkers(ticker: String, candleSeries: CandleSeries): Flow<List<TradeMarker>> {

        fun Instant.markerTime(): Instant {
            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
            return candleSeries[markerCandleIndex].openInstant
        }

        val instantRange = candleSeries.instantRange.value ?: return emptyFlow()

        val reviewProfile = appPrefs.getLongOrNullFlow(PrefKeys.TradeReviewTradingProfile)
            .map { it?.let(::ProfileId) }
            .flatMapLatest { id -> if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null) }
            .filterNotNull()

        return reviewProfile
            .flatMapLatest { profile ->
                markedTradeIds.flatMapLatest { tradeIds ->

                    val tradingRecord = tradingProfiles.getRecord(profile.id)

                    tradingRecord.trades
                        .getByTickerAndIdsInInterval(ticker, tradeIds.toList(), instantRange)
                        .map { trades ->

                            trades.filter { it.isClosed }.mapNotNull { trade ->

                                val stop = tradingRecord.trades.getStopsForTrade(trade.id).first().maxByOrNull {
                                    (trade.averageEntry - it.price).abs()
                                } ?: return@mapNotNull null
                                val target = tradingRecord.trades.getTargetsForTrade(trade.id).first().maxByOrNull {
                                    (trade.averageEntry - it.price).abs()
                                } ?: return@mapNotNull null

                                TradeMarker(
                                    entryPrice = trade.averageEntry,
                                    exitPrice = trade.averageExit!!,
                                    entryInstant = trade.entryTimestamp.markerTime(),
                                    exitInstant = trade.exitTimestamp!!.markerTime(),
                                    stopPrice = stop.price,
                                    targetPrice = target.price,
                                )
                            }
                        }
                }
            }.flowOn(Dispatchers.IO)
    }

    fun getTradeExecutionMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<TradeExecutionMarker>> {

        fun Instant.markerTime(): Instant {
            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
            return candleSeries[markerCandleIndex].openInstant
        }

        val instantRange = candleSeries.instantRange.value ?: return emptyFlow()

        val reviewProfile = appPrefs.getLongOrNullFlow(PrefKeys.TradeReviewTradingProfile)
            .map { it?.let(::ProfileId) }
            .flatMapLatest { id -> if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null) }
            .filterNotNull()

        val executionMarkers = reviewProfile
            .flatMapLatest { profile ->
                markedTradeIds.flatMapLatest { tradeIds ->

                    val tradingRecord = tradingProfiles.getRecord(profile.id)

                    tradingRecord.executions.getExecutionsByTickerAndTradeIdsInInterval(
                        ticker = ticker,
                        ids = tradeIds.toList(),
                        range = instantRange,
                    )
                }
            }
            .mapList { execution ->

                TradeExecutionMarker(
                    instant = execution.timestamp.markerTime(),
                    side = execution.side,
                    price = execution.price,
                )
            }

        return executionMarkers.flowOn(Dispatchers.IO)
    }

    fun markTrade(id: TradeId) {
        markedTradeIds.value += id
    }

    fun unMarkTrade(id: TradeId) {
        markedTradeIds.value -= id
    }

    fun clearMarkedTrades() {
        markedTradeIds.value = emptySet()
    }
}
