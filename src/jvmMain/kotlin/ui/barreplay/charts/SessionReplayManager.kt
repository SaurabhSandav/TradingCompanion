package ui.barreplay.charts

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import trading.Candle
import trading.Timeframe
import trading.barreplay.ReplaySession
import trading.dailySessionStart
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator

internal data class SessionReplayManager(
    val session: ReplaySession,
    val timeframe: Timeframe,
    val chartState: ReplayChartBridge,
) {

    val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val chartCandleSeries = when (timeframe) {
        session.inputSeries.timeframe!! -> session.replaySeries
        else -> session.resampled(timeframe)
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

    private fun setInitialData() {

        val data = chartCandleSeries.mapIndexed { index, candle ->
            ReplayChartBridge.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        }

        chartState.setData(data)
    }

    private fun update(candle: Candle) {

        val index = chartCandleSeries.indexOf(candle)

        chartState.update(
            ReplayChartBridge.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        )
    }
}
