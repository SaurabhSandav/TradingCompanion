package com.saurabhsandav.core.ui.barreplay.session

import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.DailySessionChecker
import com.saurabhsandav.core.trading.SessionChecker
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.NIFTY500
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

internal class ReplayChartsMarketDataProvider(
    private val coroutineScope: CoroutineScope,
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
        )
    }

    override fun releaseCandleSource(candleSource: CandleSource) = coroutineScope.launchUnit {
        replaySeriesCache.releaseForChart(candleSource.params)
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

            val trades = tradingProfiles.getRecord(profile.id).trades

            trades
                .getByTickerInInterval(ticker, instantRange)
                .flatMapLatest { tradesToMark ->

                    val closedTrades = tradesToMark.filter { it.isClosed }
                    val closedTradesIds = closedTrades.map { it.id }

                    combine(
                        trades.getPrimaryStops(closedTradesIds),
                        trades.getPrimaryTargets(closedTradesIds),
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
        }.flowOn(Dispatchers.IO)
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
                .getExecutionsByTickerInInterval(ticker, instantRange)
                .mapList { execution ->

                    TradeExecutionMarker(
                        instant = execution.timestamp,
                        side = execution.side,
                        price = execution.price,
                    )
                }
        }.flowOn(Dispatchers.IO)
    }
}
