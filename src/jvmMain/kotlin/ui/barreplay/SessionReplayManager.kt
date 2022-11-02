package ui.barreplay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import trading.Candle
import trading.CandleSeries
import trading.Timeframe
import trading.barreplay.BarReplay
import trading.dailySessionStart
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator

internal data class SessionReplayManager(
    val barReplay: BarReplay,
    val session: BarReplay.Session,
    val sessionParams: SessionParams,
    val chartState: ReplayChartState,
) {

    val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val chartCandleSeries = when (sessionParams.timeframe) {
        session.replayCandleSeries.timeframe!! -> session.replayCandleSeries
        else -> session.replayCandleSeries.resample(coroutineScope, sessionParams.timeframe, ::dailySessionStart)
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

    data class SessionParams(
        val symbol: String,
        val timeframe: Timeframe,
        val dataFrom: Instant,
        val dataTo: Instant,
        val replayFrom: Instant,
    )
}

private fun CandleSeries.resample(
    coroutineScope: CoroutineScope,
    timeframe: Timeframe,
    isSessionStart: (CandleSeries, Int) -> Boolean,
): CandleSeries {

    val resampledCandleSeries = CandleSeries(
        initial = getCandlesBySession(isSessionStart).map { candles ->
            candles.reduce { resampledCandle, newCandle -> resampledCandle.resample(newCandle) }
        },
        timeframe = Timeframe.D1,
    )

    coroutineScope.launch {
        live.collect { candle ->

            // If session start, Add candle, else, Resample already added candle
            val resampledCandle = when {
                isSessionStart(this@resample, lastIndex) -> candle
                else -> resampledCandleSeries.last().resample(candle)
            }

            // Add candle
            resampledCandleSeries.addCandle(resampledCandle)
        }
    }

    return resampledCandleSeries
}

private fun CandleSeries.getCandlesBySession(
    isSessionStart: (CandleSeries, Int) -> Boolean,
): List<List<Candle>> {

    // If no candles then no candles
    if (isEmpty()) return emptyList()

    val result = mutableListOf<List<Candle>>()

    var currentSessionStartIndex = 0

    // Add a list of all candles for every session to result
    indices.forEach { index ->

        // If it's session start and index is 0, there is no session before it to add
        if (isSessionStart(this, index) && index != 0) {

            // Add previous session
            result.add(subList(currentSessionStartIndex, index))

            // Remember current session start
            currentSessionStartIndex = index
        }
    }

    // Add candles for last session
    result.add(subList(currentSessionStartIndex, size))

    return result
}

private fun Candle.resample(newCandle: Candle): Candle = copy(
    high = if (high > newCandle.high) high else newCandle.high,
    low = if (low < newCandle.low) low else newCandle.low,
    close = newCandle.close,
    volume = volume + newCandle.volume,
)
