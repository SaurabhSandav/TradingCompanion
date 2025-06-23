package com.saurabhsandav.core.ui.barreplay.session

import com.github.michaelbull.result.coroutines.coroutineBinding
import com.saurabhsandav.core.trading.barreplay.BarReplay
import com.saurabhsandav.core.trading.barreplay.ReplaySeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.barreplay.model.BarReplayState
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.MutableCandleSeries
import com.saurabhsandav.trading.core.Timeframe
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first

internal class ReplaySeriesCache(
    private val replayParams: BarReplayState.ReplayParams,
    private val barReplay: BarReplay,
    private val candleRepo: CandleRepository,
) {

    private val ordersManagerReplaySeriesMap = mutableMapOf<String, ReplaySeries>()

    suspend fun getForChart(params: StockChartParams): ReplaySeries = buildReplaySeries(params.ticker, params.timeframe)

    fun releaseForChart(replaySeries: ReplaySeries) {
        barReplay.removeSeries(replaySeries)
    }

    suspend fun getForOrdersManager(ticker: String): ReplaySeries {
        return ordersManagerReplaySeriesMap.getOrPut(ticker) {
            buildReplaySeries(ticker, replayParams.baseTimeframe)
        }
    }

    fun releaseForOrdersManager(ticker: String) {

        ordersManagerReplaySeriesMap
            .remove(ticker)
            ?.let(barReplay::removeSeries)
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
