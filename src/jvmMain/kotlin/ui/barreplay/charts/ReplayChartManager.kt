package ui.barreplay.charts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import trading.Candle
import trading.Timeframe
import trading.barreplay.BarReplaySession
import trading.dailySessionStart
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.barreplay.charts.ui.ReplayChart

internal data class ReplayChartManager(
    val chartId: Int,
    val symbol: String,
    val timeframe: Timeframe,
    val chart: ReplayChart,
    val replaySession: BarReplaySession,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val ema9Indicator = EMAIndicator(ClosePriceIndicator(replaySession.replaySeries), length = 9)
    private val vwapIndicator = VWAPIndicator(replaySession.replaySeries, ::dailySessionStart)

    init {
        setInitialData()

        coroutineScope.launch {
            replaySession.replaySeries.live.collect(::update)
        }
    }

    fun reset() {
        setInitialData()
    }

    fun unsubscribeLiveCandles() {
        coroutineScope.cancel()
    }

    private fun setInitialData() {

        val data = replaySession.replaySeries.mapIndexed { index, candle ->
            ReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        }

        chart.setData(data, hasVolume = symbol != "NIFTY50")
    }

    private fun update(candle: Candle) {

        val index = replaySession.replaySeries.indexOf(candle)

        chart.update(
            ReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        )
    }
}
