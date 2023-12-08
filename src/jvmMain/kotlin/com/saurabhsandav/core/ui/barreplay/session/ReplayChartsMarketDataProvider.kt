package com.saurabhsandav.core.ui.barreplay.session

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trades.model.ProfileId
import com.saurabhsandav.core.trading.*
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*

internal class ReplayChartsMarketDataProvider(
    private val coroutineScope: CoroutineScope,
    private val replayParams: BarReplayState.ReplayParams,
    private val barReplay: BarReplay,
    private val appPrefs: FlowSettings,
    private val candleRepo: CandleRepository,
    private val tradingProfiles: TradingProfiles,
) : MarketDataProvider {

    override fun symbols(): StateFlow<ImmutableList<String>> {
        return MutableStateFlow(NIFTY50)
    }

    override fun timeframes(): StateFlow<ImmutableList<Timeframe>> {
        return MutableStateFlow(Timeframe.entries.toImmutableList())
    }

    override fun hasVolume(params: StockChartParams): Boolean {
        return params.ticker != "NIFTY50"
    }

    override fun buildCandleSource(params: StockChartParams): CandleSource {
        return ReplayCandleSource(
            params = params,
            replaySeriesFactory = { buildReplaySeries(params) },
            getTradeMarkers = { emptyFlow() },
            getTradeExecutionMarkers = { emptyFlow() },
            // TODO Disabled until markers are made individually mark-able
//            getTradeMarkers = { candleSeries -> getTradeMarkers(params.ticker, candleSeries) },
//            getTradeExecutionMarkers = { candleSeries -> getTradeExecutionMarkers(params.ticker, candleSeries) },
        )
    }

    override fun releaseCandleSource(candleSource: CandleSource) = coroutineScope.launchUnit {

        // Remove ReplaySeries from BarReplay
        barReplay.removeSeries((candleSource as ReplayCandleSource).replaySeries.await())
    }

    override fun sessionChecker(): SessionChecker = DailySessionChecker

    private suspend fun buildReplaySeries(params: StockChartParams): ReplaySeries {

        val candleSeries = getCandleSeries(params.ticker, replayParams.baseTimeframe)

        return barReplay.newSeries(
            inputSeries = candleSeries,
            initialIndex = candleSeries
                .binarySearchByAsResult(replayParams.replayFrom) { it.openInstant }
                .indexOrNaturalIndex,
            timeframeSeries = when (replayParams.baseTimeframe) {
                params.timeframe -> null
                else -> getCandleSeries(params.ticker, params.timeframe)
            },
        )
    }

    private suspend fun getCandleSeries(
        ticker: String,
        timeframe: Timeframe,
    ): CandleSeries {

        val allCandlesResult = binding {

            val candlesBefore = async {
                candleRepo.getCandlesBefore(
                    ticker = ticker,
                    timeframe = timeframe,
                    at = replayParams.replayFrom,
                    count = replayParams.candlesBefore,
                    includeAt = true,
                ).bind().first()
            }

            val candlesAfter = async {
                candleRepo.getCandles(
                    ticker = ticker,
                    timeframe = timeframe,
                    from = replayParams.replayFrom,
                    to = replayParams.dataTo,
                    includeFromCandle = false,
                ).bind().first()
            }

            candlesBefore.await() + candlesAfter.await()
        }

        return when (allCandlesResult) {
            is Ok -> MutableCandleSeries(allCandlesResult.value, timeframe)
            is Err -> when (val error = allCandlesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    private fun getTradeMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<TradeMarker>> {

        val instantRange = candleSeries.instantRange.value ?: return emptyFlow()

        val replayProfile = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
            .map { it?.let(::ProfileId) }
            .flatMapLatest { id -> if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null) }
            .filterNotNull()

        return replayProfile.flatMapLatest { profile ->

            val tradingRecord = tradingProfiles.getRecord(profile.id)

            tradingRecord.trades
                .getByTickerInInterval(ticker, instantRange)
                .map { trades ->

                    trades.filter { it.isClosed }.mapNotNull { trade ->

                        val stop = tradingRecord.trades.getPrimaryStop(trade.id).first() ?: return@mapNotNull null
                        val target = tradingRecord.trades.getPrimaryTarget(trade.id).first() ?: return@mapNotNull null

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
        }.flowOn(Dispatchers.IO)
    }

    private fun getTradeExecutionMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<TradeExecutionMarker>> {

        val instantRange = candleSeries.instantRange.value ?: return emptyFlow()

        val replayProfile = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
            .map { it?.let(::ProfileId) }
            .flatMapLatest { id -> if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null) }
            .filterNotNull()

        return replayProfile.flatMapLatest { profile ->

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
