package com.saurabhsandav.core.ui.charts

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.trading.data.CandleRepository
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import com.saurabhsandav.core.utils.retryIOResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal class ChartsCandleSource(
    override val params: StockChartParams,
    private val candleRepo: CandleRepository,
    private val getMarkers: (CandleSeries) -> Flow<List<SeriesMarker>>,
) : CandleSource {

    private val mutableCandleSeries = MutableCandleSeries(timeframe = params.timeframe)
    private val candleSeries: CandleSeries = mutableCandleSeries.asCandleSeries()

    override suspend fun getCandleSeries(): CandleSeries = candleSeries

    override suspend fun onLoad() {

        val candles = getCandles {

            candleRepo.getCandlesBefore(
                ticker = params.ticker,
                timeframe = params.timeframe,
                at = Clock.System.now(),
                count = ChartsCandleLoadCount,
                includeAt = true,
            )
        }

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

            val intervalCandles = getCandles {

                candleRepo.getCandles(
                    ticker = params.ticker,
                    timeframe = params.timeframe,
                    from = instant,
                    to = mutableCandleSeries.first().openInstant,
                    edgeCandlesInclusive = false,
                )
            }

            if (intervalCandles.isNotEmpty()) {

                val bufferCandles = when {
                    bufferCount == null -> emptyList()
                    else -> getCandles {

                        candleRepo.getCandlesBefore(
                            ticker = params.ticker,
                            timeframe = params.timeframe,
                            at = intervalCandles.first().openInstant,
                            count = bufferCount,
                            includeAt = false,
                        )
                    }
                }

                mutableCandleSeries.prependCandles(bufferCandles + intervalCandles)
            }
        }
    }

    override suspend fun onLoadBefore() {

        val oldCandles = getCandles {

            candleRepo.getCandlesBefore(
                ticker = params.ticker,
                timeframe = params.timeframe,
                at = mutableCandleSeries.first().openInstant,
                count = ChartsCandleLoadCount,
                includeAt = false,
            )
        }

        mutableCandleSeries.prependCandles(oldCandles)
    }

    override fun getCandleMarkers(): Flow<List<SeriesMarker>> = getMarkers(candleSeries)

    private suspend fun getCandles(
        request: suspend () -> Result<List<Candle>, CandleRepository.Error>,
    ): List<Candle> {

        // Suspend until logged in
        candleRepo.isLoggedIn().first { it }

        // Retry until request successful
        val candlesResult = retryIOResult(
            initialDelay = 1000,
            maxDelay = 10000,
            block = request,
        )

        return when (candlesResult) {
            is Ok -> candlesResult.value
            is Err -> when (val error = candlesResult.error) {
                is CandleRepository.Error.AuthError -> error(error.message ?: "AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }
}

private const val ChartsCandleLoadCount = 500
