package com.saurabhsandav.core.ui.charts

import com.github.michaelbull.result.Result
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.retryIOResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant

internal class ChartsCandleSource(
    override val params: StockChartParams,
    private val candleRepo: CandleRepository,
    private val getTradeMarkers: (ClosedRange<Instant>) -> Flow<List<TradeMarker>>,
    private val getTradeExecutionMarkers: (ClosedRange<Instant>) -> Flow<List<TradeExecutionMarker>>,
) : CandleSource {

    override suspend fun onLoad(interval: ClosedRange<Instant>): CandleSource.Result {

        val candles = unwrap {

            candleRepo.getCandles(
                ticker = params.ticker,
                timeframe = params.timeframe,
                from = interval.start,
                to = interval.endInclusive,
                includeFromCandle = false,
            )
        }.first()

        return CandleSource.Result(candles)
    }

    override suspend fun onLoadBefore(before: Instant, count: Int): CandleSource.Result {

        val candles = unwrap {

            candleRepo.getCandlesBefore(
                ticker = params.ticker,
                timeframe = params.timeframe,
                at = before,
                count = count,
                includeAt = true,
            )
        }.first()

        return CandleSource.Result(candles)
    }

    override suspend fun onLoadAfter(after: Instant, count: Int): CandleSource.Result {

        val candles = unwrap {

            candleRepo.getCandlesAfter(
                ticker = params.ticker,
                timeframe = params.timeframe,
                at = after,
                count = count,
                includeAt = true,
            )
        }.first()

        return CandleSource.Result(candles)
    }

    override fun getTradeMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeMarker>> {
        return getTradeMarkers.invoke(instantRange)
    }

    override fun getTradeExecutionMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeExecutionMarker>> {
        return getTradeExecutionMarkers.invoke(instantRange)
    }

    private suspend fun <T> unwrap(
        request: suspend () -> Result<T, CandleRepository.Error>,
    ): T {

        // Suspend until logged in
        candleRepo.isLoggedIn().first { it }

        // Retry until request successful
        val result = retryIOResult(
            initialDelay = 1000,
            maxDelay = 10000,
            block = request,
        )

        return when {
            result.isOk -> result.value
            else -> when (val error = result.error) {
                is CandleRepository.Error.AuthError -> error(error.message ?: "AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }
}
