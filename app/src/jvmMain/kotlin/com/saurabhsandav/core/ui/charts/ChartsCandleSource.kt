package com.saurabhsandav.core.ui.charts

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.data.CandleSource
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.retryIOResult
import com.saurabhsandav.trading.candledata.CandleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Instant

internal class ChartsCandleSource(
    override val params: StockChartParams,
    private val candleRepo: CandleRepository,
    private val getTradeMarkers: (ClosedRange<Instant>) -> Flow<List<TradeMarker>>,
    private val getTradeExecutionMarkers: (ClosedRange<Instant>) -> Flow<List<TradeExecutionMarker>>,
) : CandleSource {

    override suspend fun onLoad(interval: ClosedRange<Instant>): CandleSource.Result {

        val candles = unwrap {

            candleRepo.getCandles(
                symbolId = params.symbolId,
                timeframe = params.timeframe,
                from = interval.start,
                to = interval.endInclusive,
                includeFromCandle = true,
            )
        }

        return CandleSource.Result(candles)
    }

    override suspend fun getCount(interval: ClosedRange<Instant>): Int {
        return unwrap {

            candleRepo.getCountInRange(
                symbolId = params.symbolId,
                timeframe = params.timeframe,
                from = interval.start,
                to = interval.endInclusive,
            )
        }.first().toInt()
    }

    override suspend fun getBeforeInstant(
        currentBefore: Instant,
        loadCount: Int,
    ): Instant? {

        return unwrap {

            candleRepo.getInstantBeforeByCount(
                symbolId = params.symbolId,
                timeframe = params.timeframe,
                before = currentBefore,
                count = loadCount,
            )
        }.first()
    }

    override suspend fun getAfterInstant(
        currentAfter: Instant,
        loadCount: Int,
    ): Instant? {

        return unwrap {

            candleRepo.getInstantAfterByCount(
                symbolId = params.symbolId,
                timeframe = params.timeframe,
                after = currentAfter,
                count = loadCount,
            )
        }.first()
    }

    override fun getTradeMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeMarker>> {
        return getTradeMarkers.invoke(instantRange)
    }

    override fun getTradeExecutionMarkers(instantRange: ClosedRange<Instant>): Flow<List<TradeExecutionMarker>> {
        return getTradeExecutionMarkers.invoke(instantRange)
    }

    private suspend fun <T> unwrap(request: suspend () -> Result<T, CandleRepository.Error>): T {

        // Suspend until logged in
        candleRepo.isLoggedIn().first { it }

        // Retry until request successful
        val result = retryIOResult(
            initialDelay = 1000,
            maxDelay = 10000,
            block = request,
        )

        return result.fold(
            success = { candles -> candles },
            failure = { error ->
                when (error) {
                    is CandleRepository.Error.AuthError -> error(error.message ?: "AuthError")
                    is CandleRepository.Error.UnknownError -> error(error.message)
                }
            },
        )
    }
}
