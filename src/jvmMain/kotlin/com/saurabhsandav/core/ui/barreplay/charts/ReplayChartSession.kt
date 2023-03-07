package com.saurabhsandav.core.ui.barreplay.charts

import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.Timeframe
import com.saurabhsandav.core.trading.barreplay.BarReplaySession
import com.saurabhsandav.core.ui.stockchart.CandleSource
import com.saurabhsandav.core.ui.stockchart.StockChart
import kotlinx.coroutines.flow.MutableSharedFlow

internal class ReplayChartSession(
    val stockChart: StockChart,
    private val replaySessionBuilder: suspend (String, Timeframe) -> BarReplaySession,
) {

    val replaySession = MutableSharedFlow<BarReplaySession>(replay = 1)

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

            private var _candleSeries: CandleSeries? = null
            override val candleSeries: CandleSeries
                get() = checkNotNull(_candleSeries) { "CandleSeries not loaded" }

            override suspend fun onLoad() {
                val newReplaySession = replaySessionBuilder(ticker, timeframe)
                replaySession.tryEmit(newReplaySession)
                _candleSeries = newReplaySession.replaySeries
            }
        }

        stockChart.setCandleSource(candleSource)
    }
}
