package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.trading.*
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

internal class ChartSession(
    val stockChart: StockChart,
    private val getCandles: suspend (String, Timeframe, ClosedRange<Instant>) -> List<Candle>,
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

            val mutableCandleSeries = MutableCandleSeries(timeframe = timeframe)
            override val candleSeries: CandleSeries = mutableCandleSeries.asCandleSeries()

            override suspend fun onLoad() {

                // Range of 3 months before current time to current time
                val candles = getCandles(
                    ticker,
                    timeframe,
                    run {
                        val currentTime = Clock.System.now()
                        currentTime.minus(downloadIntervalDays)..currentTime
                    }
                )

                candles.forEach(mutableCandleSeries::addCandle)
            }

            override suspend fun onLoadBefore(): Boolean {

                val firstCandleInstant = mutableCandleSeries.first().openInstant

                val oldCandles = getCandles(
                    ticker,
                    timeframe,
                    firstCandleInstant.minus(downloadIntervalDays)..firstCandleInstant,
                )

                val areCandlesAvailable = oldCandles.isNotEmpty()

                if (areCandlesAvailable) mutableCandleSeries.prependCandles(oldCandles)

                return areCandlesAvailable
            }
        }

        stockChart.setCandleSource(candleSource)
    }
}
