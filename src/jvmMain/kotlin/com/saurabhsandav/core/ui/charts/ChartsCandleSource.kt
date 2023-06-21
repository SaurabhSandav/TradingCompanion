package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChartParams
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesMarker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

internal class ChartsCandleSource(
    override val params: StockChartParams,
    private val getCandles: suspend (ClosedRange<Instant>) -> List<Candle>,
    private val getMarkers: suspend (CandleSeries) -> Flow<List<SeriesMarker>>,
) : CandleSource {

    private val downloadIntervalDays = 90.days
    private var loaded = false
    private val onLoadSignal = MutableSharedFlow<Unit>(replay = 1)
    private val mutex = Mutex()

    override val hasVolume: Boolean = params.ticker != "NIFTY50"

    private val mutableCandleSeries = MutableCandleSeries(timeframe = params.timeframe)
    override val candleSeries: CandleSeries = mutableCandleSeries.asCandleSeries()

    override val candleMarkers: Flow<List<SeriesMarker>> = onLoadSignal.flatMapLatest { getMarkers(candleSeries) }

    override suspend fun onLoad() = mutex.withLock {

        if (loaded) return

        val currentTime = Clock.System.now()
        val range = currentTime.minus(downloadIntervalDays)..currentTime

        // Range of 3 months before current time to current time
        val candles = getCandles(range)

        // Append candles
        mutableCandleSeries.appendCandles(candles)

        onLoadSignal.tryEmit(Unit)

        loaded = true
    }

    override suspend fun onLoad(start: Instant, end: Instant?): Boolean = mutex.withLock {

        val firstCandleInstant = candleSeries.firstOrNull()?.openInstant
        val isBefore = firstCandleInstant != null && start < firstCandleInstant

        if (isBefore) {

            // New range starts 1 month before given date
            val rangeStart = start.minus(30.days)
            val rangeEnd = mutableCandleSeries.first().openInstant
            val range = rangeStart..rangeEnd

            val oldCandles = getCandles(range)

            if (oldCandles.isNotEmpty()) {
                mutableCandleSeries.prependCandles(oldCandles)
                onLoadSignal.tryEmit(Unit)
                return true
            }
        }

        return false
    }

    override suspend fun onLoadBefore(): Boolean {

        // If locked, loading before may be unnecessary.
        if (mutex.isLocked) return false

        mutex.withLock {

            val firstCandleInstant = mutableCandleSeries.first().openInstant
            val range = firstCandleInstant.minus(downloadIntervalDays)..firstCandleInstant

            val oldCandles = getCandles(range)

            val areCandlesAvailable = oldCandles.isNotEmpty()

            if (areCandlesAvailable) {
                mutableCandleSeries.prependCandles(oldCandles)
                onLoadSignal.tryEmit(Unit)
            }

            return areCandlesAvailable
        }
    }
}

fun interface ChartMarkersProvider {

    fun provideMarkers(ticker: String, candleSeries: CandleSeries): Flow<List<SeriesMarker>>
}
