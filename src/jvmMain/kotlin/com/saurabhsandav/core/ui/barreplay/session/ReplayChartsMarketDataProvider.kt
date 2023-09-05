package com.saurabhsandav.core.ui.barreplay.session

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.coroutines.binding.binding
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.trades.TradingProfiles
import com.saurabhsandav.core.trading.*
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.MarketDataProvider
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeOrderMarker
import com.saurabhsandav.core.utils.NIFTY50
import com.saurabhsandav.core.utils.PrefKeys
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal class ReplayChartsMarketDataProvider(
    private val coroutineScope: CoroutineScope,
    private val replayParams: BarReplayState.ReplayParams,
    private val barReplay: BarReplay,
    private val appModule: AppModule,
    private val appPrefs: FlowSettings = appModule.appPrefs,
    private val candleRepo: CandleRepository = appModule.candleRepo,
    private val tradingProfiles: TradingProfiles = appModule.tradingProfiles,
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
            getMarkers = { candleSeries -> getMarkers(params.ticker, candleSeries) },
        )
    }

    override fun releaseCandleSource(candleSource: CandleSource) = coroutineScope.launchUnit {

        // Remove ReplaySeries from BarReplay
        barReplay.removeSeries((candleSource as ReplayCandleSource).replaySeries.await())
    }

    override suspend fun sessionChecker(): SessionChecker = DailySessionChecker

    private suspend fun buildReplaySeries(params: StockChartParams): ReplaySeries {

        val candleSeries = getCandleSeries(params.ticker, replayParams.baseTimeframe)

        return barReplay.newSeries(
            inputSeries = candleSeries,
            initialIndex = candleSeries.indexOfFirst { it.openInstant >= replayParams.replayFrom },
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
                ).bind()
            }

            val candlesAfter = async {
                candleRepo.getCandles(
                    ticker = ticker,
                    timeframe = timeframe,
                    from = replayParams.replayFrom,
                    to = replayParams.dataTo,
                ).bind()
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

    private fun getMarkers(
        ticker: String,
        candleSeries: CandleSeries,
    ): Flow<List<SeriesMarker>> {

        fun Instant.markerTime(): Instant {
            val markerCandleIndex = candleSeries.indexOfLast { it.openInstant <= this }
            return candleSeries[markerCandleIndex].openInstant
        }

        val replayProfile = appPrefs.getLongOrNullFlow(PrefKeys.ReplayTradingProfile)
            .flatMapLatest { id -> if (id != null) tradingProfiles.getProfileOrNull(id) else flowOf(null) }
            .filterNotNull()

        val orderMarkers = replayProfile.flatMapLatest { profile ->

            val tradingRecord = tradingProfiles.getRecord(profile.id)

            candleSeries.instantRange.flatMapLatest test@{ instantRange ->

                instantRange ?: return@test emptyFlow()

                val ldtRange = instantRange.start.toLocalDateTime(TimeZone.currentSystemDefault())..
                        instantRange.endInclusive.toLocalDateTime(TimeZone.currentSystemDefault())

                tradingRecord.executions.getExecutionsByTickerInInterval(
                    ticker,
                    ldtRange,
                )
            }
        }
            .mapList { order ->

                val orderInstant = order.timestamp.toInstant(TimeZone.currentSystemDefault())

                TradeOrderMarker(
                    instant = orderInstant.markerTime(),
                    side = order.side,
                    price = order.price,
                )
            }

        val tradeMarkers = replayProfile.flatMapLatest { profile ->

            val tradingRecord = tradingProfiles.getRecord(profile.id)

            candleSeries.instantRange.flatMapLatest test@{ instantRange ->

                instantRange ?: return@test emptyFlow()

                val ldtRange = instantRange.start.toLocalDateTime(TimeZone.currentSystemDefault())..
                        instantRange.endInclusive.toLocalDateTime(TimeZone.currentSystemDefault())

                tradingRecord.trades.getByTickerInInterval(ticker, ldtRange)
            }
        }
            .map { trades ->
                trades.flatMap { trade ->

                    val entryInstant = trade.entryTimestamp.toInstant(TimeZone.currentSystemDefault())

                    buildList {

                        add(
                            TradeMarker(
                                instant = entryInstant.markerTime(),
                                isEntry = true,
                            )
                        )

                        if (trade.isClosed) {

                            val exitInstant = trade.exitTimestamp!!.toInstant(TimeZone.currentSystemDefault())

                            add(
                                TradeMarker(
                                    instant = exitInstant.markerTime(),
                                    isEntry = false,
                                )
                            )
                        }
                    }
                }
            }

        return orderMarkers.combine(tradeMarkers) { orderMkrs, tradeMkrs -> orderMkrs + tradeMkrs }
            .flowOn(Dispatchers.IO)
    }
}
