package ui.barreplay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import trading.Candle
import trading.Timeframe
import trading.barreplay.ReplaySession
import trading.dailySessionStart
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator

internal data class SessionReplayManager(
    val session: ReplaySession,
    val sessionParams: SessionParams,
    val chartState: ReplayChartState,
) {

    val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val chartCandleSeries = when (sessionParams.timeframe) {
        session.inputSeries.timeframe!! -> session.replaySeries
        else -> session.resampled(sessionParams.timeframe)
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
            ReplayChartState.Data(
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
            ReplayChartState.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        )
    }

    data class SessionParams(
        val symbol: String,
        val timeframe: Timeframe,
        val dataFrom: Instant,
        val dataTo: Instant,
        val replayFrom: Instant,
    )
}
