package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.chart.data.SeriesMarker
import com.saurabhsandav.core.trading.*
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChart
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.days

internal class ChartSession(
    val stockChart: StockChart,
    private val getCandles: suspend (String, Timeframe, ClosedRange<Instant>) -> List<Candle>,
    private val getMarkers: suspend (String, ClosedRange<Instant>) -> Flow<List<SeriesMarker>>,
) {

    private val downloadIntervalDays = 90.days

    fun newParams(
        ticker: String? = stockChart.currentParams?.ticker,
        timeframe: Timeframe? = stockChart.currentParams?.timeframe,
    ) {

        check(ticker != null && timeframe != null) {
            "Ticker ($ticker) and/or Timeframe ($timeframe) cannot be null"
        }

        val candleSource = object : CandleSource {
            override val ticker: String = ticker
            override val timeframe: Timeframe = timeframe
            override val hasVolume: Boolean = ticker != "NIFTY50"

            private val mutableCandleSeries = MutableCandleSeries(timeframe = timeframe)
            override val candleSeries: CandleSeries = mutableCandleSeries.asCandleSeries()

            private val candleRange = MutableStateFlow<ClosedRange<Instant>?>(null)
            override val candleMarkers: Flow<List<SeriesMarker>> = candleRange.filterNotNull()
                .flatMapLatest { getMarkers(ticker, it) }

            override suspend fun onLoad() {

                val currentTime = Clock.System.now()
                val range = currentTime.minus(downloadIntervalDays)..currentTime

                // Range of 3 months before current time to current time
                val candles = getCandles(ticker, timeframe, range)

                candles.forEach(mutableCandleSeries::addCandle)

                candleRange.update { candleSeries.first().openInstant..candleSeries.last().openInstant }
            }

            override suspend fun onLoadBefore(): Boolean {

                val firstCandleInstant = mutableCandleSeries.first().openInstant
                val range = firstCandleInstant.minus(downloadIntervalDays)..firstCandleInstant

                val oldCandles = getCandles(ticker, timeframe, range)

                val areCandlesAvailable = oldCandles.isNotEmpty()

                if (areCandlesAvailable) {
                    mutableCandleSeries.prependCandles(oldCandles)
                    candleRange.update { candleSeries.first().openInstant..candleSeries.last().openInstant }
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
                        candleRange.update { candleSeries.first().openInstant..candleSeries.last().openInstant }
                        return true
                    }
                }

                return false
            }
        }

        stockChart.setCandleSource(candleSource)
    }
}
