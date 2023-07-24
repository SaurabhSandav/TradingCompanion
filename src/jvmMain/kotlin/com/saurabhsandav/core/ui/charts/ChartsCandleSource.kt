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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal class ChartsCandleSource(
    override val params: StockChartParams,
    private val candleRepo: CandleRepository,
    private val getMarkers: (CandleSeries) -> Flow<List<SeriesMarker>>,
) : CandleSource {

    private var loaded = false
    private val mutex = Mutex()

    private val mutableCandleSeries = MutableCandleSeries(timeframe = params.timeframe)
    override val candleSeries: CandleSeries = mutableCandleSeries.asCandleSeries()

    override suspend fun onLoad() = mutex.withLock {

        if (loaded) return

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

        loaded = true
    }

    override suspend fun onLoad(start: Instant, end: Instant?) = mutex.withLock {

        val firstCandleInstant = candleSeries.firstOrNull()?.openInstant
        val isBefore = firstCandleInstant != null && start < firstCandleInstant

        if (isBefore) {

            do {

                val candles = getCandles {

                    candleRepo.getCandlesBefore(
                        ticker = params.ticker,
                        timeframe = params.timeframe,
                        at = mutableCandleSeries.first().openInstant,
                        count = ChartsCandleLoadCount,
                        includeAt = false,
                    )
                }

                if (candles.isEmpty()) break

                mutableCandleSeries.prependCandles(candles)

            } while (start < candleSeries.first().openInstant)
        }
    }

    override suspend fun onLoadBefore() {

        // If locked, loading before may be unnecessary.
        if (mutex.isLocked) return

        mutex.withLock {

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

fun interface ChartMarkersProvider {

    fun provideMarkers(ticker: String, candleSeries: CandleSeries): Flow<List<SeriesMarker>>
}
