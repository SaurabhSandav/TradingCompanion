package com.saurabhsandav.core.ui.charts

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.TradeExecutionMarker
import com.saurabhsandav.core.ui.stockchart.plotter.TradeMarker
import com.saurabhsandav.core.utils.retryIOResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal class ChartsCandleSource(
    override val params: StockChartParams,
    private val candleRepo: CandleRepository,
    private val getTradeMarkers: (ClosedRange<Instant>) -> Flow<List<TradeMarker>>,
    private val getTradeExecutionMarkers: (ClosedRange<Instant>) -> Flow<List<TradeExecutionMarker>>,
) : CandleSource {

    private val mutableCandleSeries = MutableCandleSeries(timeframe = params.timeframe)
    private val candleSeries: CandleSeries = mutableCandleSeries.asCandleSeries()

    override suspend fun getCandleSeries(): CandleSeries = candleSeries

    override suspend fun onLoad() {

        val candles = unwrap {

            candleRepo.getCandlesBefore(
                ticker = params.ticker,
                timeframe = params.timeframe,
                at = Clock.System.now(),
                count = ChartsCandleLoadCount,
                includeAt = true,
            )
        }.first()

        // Append candles
        mutableCandleSeries.appendCandles(candles)
    }

    override suspend fun onLoad(
        instant: Instant,
        to: Instant?,
        bufferCount: Int?,
    ) {

        val firstCandleInstant = candleSeries.firstOrNull()?.openInstant
        val isBefore = firstCandleInstant != null && instant < firstCandleInstant

        if (isBefore) {

            val intervalCandles = unwrap {

                candleRepo.getCandles(
                    ticker = params.ticker,
                    timeframe = params.timeframe,
                    from = instant,
                    to = mutableCandleSeries.first().openInstant,
                    includeFromCandle = false,
                )
            }.first()

            if (intervalCandles.isNotEmpty()) {

                val bufferCandles = when {
                    bufferCount == null -> emptyList()
                    else -> unwrap {

                        candleRepo.getCandlesBefore(
                            ticker = params.ticker,
                            timeframe = params.timeframe,
                            at = intervalCandles.first().openInstant,
                            count = bufferCount,
                            includeAt = false,
                        )
                    }.first()
                }

                mutableCandleSeries.prependCandles(bufferCandles + intervalCandles)
            }
        }
    }

    override suspend fun onLoadBefore() {

        val oldCandles = unwrap {

            candleRepo.getCandlesBefore(
                ticker = params.ticker,
                timeframe = params.timeframe,
                at = mutableCandleSeries.first().openInstant,
                count = ChartsCandleLoadCount,
                includeAt = false,
            )
        }.first()

        mutableCandleSeries.prependCandles(oldCandles)
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

        return when (result) {
            is Ok -> result.value
            is Err -> when (val error = result.error) {
                is CandleRepository.Error.AuthError -> error(error.message ?: "AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }
}

private const val ChartsCandleLoadCount = 500
