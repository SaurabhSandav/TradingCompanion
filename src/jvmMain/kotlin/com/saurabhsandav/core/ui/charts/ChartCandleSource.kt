package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.trading.*
import com.saurabhsandav.core.ui.stockchart.CandleSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.days

class ChartCandleSource(
    override val ticker: String,
    override val timeframe: Timeframe,
    private val getCandles: suspend (String, Timeframe, ClosedRange<Instant>) -> List<Candle>,
    private val getMarkers: suspend (String, CandleSeries) -> Flow<List<SeriesMarker>>,
) : CandleSource {

    private val downloadIntervalDays = 90.days
    private var loaded = false
    private val onLoadSignal = MutableSharedFlow<Unit>(replay = 1)

    override val hasVolume: Boolean = ticker != "NIFTY50"

    private val mutableCandleSeries = MutableCandleSeries(timeframe = timeframe)
    override val candleSeries: CandleSeries = mutableCandleSeries.asCandleSeries()

    override val candleMarkers: Flow<List<SeriesMarker>> = onLoadSignal.flatMapLatest {
        getMarkers(ticker, candleSeries)
    }

    override val syncKey = timeframe

    override suspend fun onLoad() {

        if (loaded) return

        val currentTime = Clock.System.now()
        val range = currentTime.minus(downloadIntervalDays)..currentTime

        // Range of 3 months before current time to current time
        val candles = getCandles(ticker, timeframe, range)

        candles.forEach(mutableCandleSeries::addCandle)

        onLoadSignal.tryEmit(Unit)

        loaded = true
    }

    override suspend fun onLoadBefore(): Boolean {

        val firstCandleInstant = mutableCandleSeries.first().openInstant
        val range = firstCandleInstant.minus(downloadIntervalDays)..firstCandleInstant

        val oldCandles = getCandles(ticker, timeframe, range)

        val areCandlesAvailable = oldCandles.isNotEmpty()

        if (areCandlesAvailable) {
            mutableCandleSeries.prependCandles(oldCandles)
            onLoadSignal.tryEmit(Unit)
        }

        return areCandlesAvailable
    }

    override suspend fun onLoadDateTime(dateTime: LocalDateTime): Boolean {

        val instant = dateTime.toInstant(TimeZone.currentSystemDefault())

        val firstCandleLDT = candleSeries.firstOrNull()?.openInstant
            ?.toLocalDateTime(TimeZone.currentSystemDefault())
        val isBefore = firstCandleLDT != null && dateTime < firstCandleLDT

        if (isBefore) {

            // New range starts 3 months before given date
            val rangeStart = instant.minus(downloadIntervalDays)
            val rangeEnd = mutableCandleSeries.first().openInstant
            val range = rangeStart..rangeEnd

            val oldCandles = getCandles(ticker, timeframe, range)

            if (oldCandles.isNotEmpty()) {
                mutableCandleSeries.prependCandles(oldCandles)
                onLoadSignal.tryEmit(Unit)
                return true
            }
        }

        return false
    }
}
