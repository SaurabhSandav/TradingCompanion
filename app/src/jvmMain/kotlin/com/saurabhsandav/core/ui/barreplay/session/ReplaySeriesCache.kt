package com.saurabhsandav.core.ui.barreplay.session

import com.github.michaelbull.result.coroutines.coroutineBinding
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ReplaySeriesCache(
    private val replayParams: BarReplayState.ReplayParams,
    private val barReplay: BarReplay,
    private val candleRepo: CandleRepository,
) {

    private val chartReplaySeriesMap = mutableMapOf<StockChartParams, ReplaySeries>()
    private val ordersManagerReplaySeriesMap = mutableMapOf<String, ReplaySeries>()
    private val mutex = Mutex()

    suspend fun getForChart(params: StockChartParams): ReplaySeries = mutex.withLock {

        return chartReplaySeriesMap.getOrPut(params) {

            val cached = when (replayParams.baseTimeframe) {
                params.timeframe -> ordersManagerReplaySeriesMap[params.ticker]
                else -> null
            }

            cached ?: buildReplaySeries(params.ticker, params.timeframe)
        }
    }

    suspend fun releaseForChart(params: StockChartParams) = mutex.withLock {

        val replaySeries = chartReplaySeriesMap.remove(params)

        if (replaySeries != null) {

            val ordersManagerReplaySeries = when (replayParams.baseTimeframe) {
                params.timeframe -> ordersManagerReplaySeriesMap[params.ticker]
                else -> null
            }

            if (ordersManagerReplaySeries == null) barReplay.removeSeries(replaySeries)
        }
    }

    suspend fun getForOrdersManager(ticker: String): ReplaySeries = mutex.withLock {

        return ordersManagerReplaySeriesMap.getOrPut(ticker) {

            val cached = chartReplaySeriesMap[StockChartParams(ticker, replayParams.baseTimeframe)]

            cached ?: buildReplaySeries(ticker, replayParams.baseTimeframe)
        }
    }

    suspend fun releaseForOrdersManager(ticker: String) = mutex.withLock {

        val replaySeries = ordersManagerReplaySeriesMap.remove(ticker)

        if (replaySeries != null) {

            val chartReplaySeries = chartReplaySeriesMap[StockChartParams(ticker, replayParams.baseTimeframe)]

            if (chartReplaySeries == null) barReplay.removeSeries(replaySeries)
        }
    }

    private suspend fun buildReplaySeries(
        ticker: String,
        timeframe: Timeframe,
    ): ReplaySeries {

        val candleSeries = getCandleSeries(ticker, replayParams.baseTimeframe)

        return barReplay.newSeries(
            inputSeries = candleSeries,
            timeframeSeries = when (replayParams.baseTimeframe) {
                timeframe -> null
                else -> getCandleSeries(ticker, timeframe)
            },
        )
    }

    private suspend fun getCandleSeries(
        ticker: String,
        timeframe: Timeframe,
    ): CandleSeries {

        val allCandlesResult = coroutineBinding {

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

        return when {
            allCandlesResult.isOk -> MutableCandleSeries(allCandlesResult.value, timeframe)
            else -> when (val error = allCandlesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }
}
