package com.saurabhsandav.core.ui.charts

import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.trading.MutableCandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.asCandleSeries
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

internal class ChartSession(
    val tabId: Int,
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

        val mutableCandleSeries = MutableCandleSeries(timeframe = timeframe)

        val candleSource = CandleSource(
            ticker = ticker,
            timeframe = timeframe,
            hasVolume = ticker != "NIFTY50",
            onLoad = {

                // Range of 3 months before current time to current time
                val candles = getCandles(
                    ticker, timeframe, run {
                        val currentTime = Clock.System.now()
                        currentTime.minus(downloadIntervalDays)..currentTime
                    }
                )

                candles.forEach { mutableCandleSeries.addCandle(it) }
                mutableCandleSeries.asCandleSeries()
            },
            onLoadBefore = {

                val firstCandleInstant = mutableCandleSeries.first().openInstant

                val oldCandles = getCandles(
                    ticker, timeframe, firstCandleInstant.minus(downloadIntervalDays)..firstCandleInstant
                )

                val areCandlesAvailable = oldCandles.isNotEmpty()

                if (oldCandles.isNotEmpty()) {
                    mutableCandleSeries.prependCandles(oldCandles)
                }

                areCandlesAvailable
            }
        )

        stockChart.setCandleSource(candleSource)
    }
}
