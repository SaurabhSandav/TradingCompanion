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

internal data class ReplayDataManager(
    val chart: ReplayChart,
    val replaySession: BarReplaySession,
    val timeframe: Timeframe,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val chartCandleSeries = when (timeframe) {
        replaySession.inputSeries.timeframe!! -> replaySession.replaySeries
        else -> replaySession.resampled(timeframe)
    }

    private val ema9Indicator = EMAIndicator(ClosePriceIndicator(chartCandleSeries), length = 9)
    private val vwapIndicator = VWAPIndicator(chartCandleSeries, ::dailySessionStart)

    init {
        setInitialData()

        coroutineScope.launch {
            chartCandleSeries.live.collect(::update)
        }
    }

    fun reset() {
        setInitialData()
    }

    fun unsubscribeLiveCandles() {
        coroutineScope.cancel()
    }

    private fun setInitialData() {

        val data = chartCandleSeries.mapIndexed { index, candle ->
            ReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        }

        chart.setData(data)
    }

    private fun update(candle: Candle) {

        val index = chartCandleSeries.indexOf(candle)

        chart.update(
            ReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        )
    }
}
