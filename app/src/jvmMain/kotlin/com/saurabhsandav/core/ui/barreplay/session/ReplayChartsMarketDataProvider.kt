package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.trading.SessionChecker
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.data.CandleSource
import com.saurabhsandav.core.ui.stockchart.data.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.AppDispatchers
import com.saurabhsandav.core.utils.NIFTY500
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

internal class ReplayChartsMarketDataProvider(
    private val appDispatchers: AppDispatchers,
    private val profileId: ProfileId?,
    private val replaySeriesCache: ReplaySeriesCache,
    private val tradingProfiles: TradingProfiles,
) : MarketDataProvider {

    override fun symbols(): StateFlow<List<String>> {
        return MutableStateFlow(NIFTY500)
    }

    override fun timeframes(): StateFlow<List<Timeframe>> {
        return MutableStateFlow(Timeframe.entries.toList())
    }

    override fun hasVolume(params: StockChartParams): Boolean {
        return params.ticker != "NIFTY50"
    }

    override fun buildCandleSource(params: StockChartParams): CandleSource {
        return ReplayCandleSource(
            params = params,
            replaySeriesFactory = { replaySeriesCache.getForChart(params) },
            getTradeMarkers = { emptyFlow() },
            getTradeExecutionMarkers = { emptyFlow() },
            // TODO Disabled until markers are made individually mark-able
//            getTradeMarkers = { candleSeries -> getTradeMarkers(params.ticker, candleSeries) },
//            getTradeExecutionMarkers = { candleSeries -> getTradeExecutionMarkers(params.ticker, candleSeries) },
            onDestroy = replaySeriesCache::releaseForChart,
        )
    }

    override fun sessionChecker(): SessionChecker = DailySessionChecker

    private fun getTradeMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<TradeMarker>> {

        val instantRange = candleSeries.instantRange.value ?: return emptyFlow()

        val replayProfile = when {
            profileId != null -> tradingProfiles.getProfileOrNull(profileId)
            else -> flowOf(null)
        }

        return replayProfile.flatMapLatest { profile ->

            if (profile == null) return@flatMapLatest flowOf(emptyList())

            val tradingRecord = tradingProfiles.getRecord(profile.id)

            tradingRecord.trades
                .getByTickerInInterval(ticker, instantRange)
                .flatMapLatest { tradesToMark ->

                    val closedTrades = tradesToMark.filter { it.isClosed }
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
        }.flowOn(appDispatchers.IO)
    }

    private fun getTradeExecutionMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<TradeExecutionMarker>> {

        val instantRange = candleSeries.instantRange.value ?: return emptyFlow()

        val replayProfile = when {
            profileId != null -> tradingProfiles.getProfileOrNull(profileId)
            else -> flowOf(null)
        }

        return replayProfile.flatMapLatest { profile ->

            if (profile == null) return@flatMapLatest flowOf(emptyList())

            val tradingRecord = tradingProfiles.getRecord(profile.id)

            tradingRecord.executions
                .getByTickerInInterval(ticker, instantRange)
                .mapList { execution ->

                    TradeExecutionMarker(
                        instant = execution.timestamp,
                        side = execution.side,
                        price = execution.price,
                    )
                }
        }.flowOn(appDispatchers.IO)
    }
}
