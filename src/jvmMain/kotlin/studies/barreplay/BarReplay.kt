package studies.barreplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import trading.CandleSeries
import trading.Timeframe
import trading.data.CandleRepository
import trading.defaultIsSessionStart
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import kotlin.time.Duration.Companion.seconds

internal class BarReplay(
    private val candleRepo: CandleRepository,
    private val coroutineScope: CoroutineScope,
    private val replayChart: BarReplayChart,
    private val symbol: String,
    private val timeframe: Timeframe,
    private val dataFrom: Instant,
    private val dataTo: Instant,
    private val replayFrom: Instant,
) {

    private var currentSymbol = symbol
    private var currentTimeframe = timeframe
    private var initialCandleIndex: Int = -1

    private lateinit var candleSeries: CandleSeries
    private lateinit var baseCandleSeries: CandleSeries
    private lateinit var ema9Indicator: EMAIndicator
    private lateinit var vwapIndicator: VWAPIndicator

    private val replayClock = BarReplayClock()

    var isAutoNextEnabled by mutableStateOf(false)

    init {

        coroutineScope.launch {
            snapshotFlow { isAutoNextEnabled }.collectLatest {
                while (it) {
                    delay(1.seconds)
                    next()
                }
            }
        }
    }

    suspend fun init() {

        candleSeries = getCandleSeries(symbol, timeframe)
        baseCandleSeries = candleSeries

        initialCandleIndex = candleSeries.list.indexOfFirst { it.openInstant >= replayFrom }

        ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        vwapIndicator = VWAPIndicator(candleSeries, ::defaultIsSessionStart)

        setInitialData()
    }

    fun newSymbol(symbol: String) = coroutineScope.launch {

        currentSymbol = symbol

        candleSeries = getCandleSeries(symbol, currentTimeframe)
        baseCandleSeries = candleSeries
        ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        vwapIndicator = VWAPIndicator(candleSeries, ::defaultIsSessionStart)

        setInitialData()
    }

    fun newTimeframe(timeframe: Timeframe) = coroutineScope.launch {

        if (timeframe.seconds < this@BarReplay.timeframe.seconds) error("New Timeframe cannot be less than Base Timeframe")

        currentTimeframe = timeframe

        candleSeries = getCandleSeries(symbol, timeframe)
        ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        vwapIndicator = VWAPIndicator(candleSeries, ::defaultIsSessionStart)

        setInitialData()
    }

    fun reset() {

        replayClock.reset()
        isAutoNextEnabled = false

        setInitialData()
    }

    fun next() {

        replayClock.next()

        val currentInstant = baseCandleSeries.list[initialCandleIndex + replayClock.currentOffset].openInstant

        val index = when (currentTimeframe) {
            timeframe -> initialCandleIndex + replayClock.currentOffset
            else -> candleSeries.list.indexOfFirst {
                val candleCloseInstant = it.openInstant + (currentTimeframe.seconds - 1).seconds
                currentInstant in it.openInstant..candleCloseInstant
            }
        }

        val candle = when (currentTimeframe) {
            timeframe -> candleSeries.list[index]
            else -> {

                val adjustedCandleOpenInstant = candleSeries.list[index].openInstant
                val timeframeCandles = baseCandleSeries.list.filter {
                    it.openInstant in adjustedCandleOpenInstant..currentInstant
                }

                var currentCandle = timeframeCandles.first().copy(openInstant = adjustedCandleOpenInstant)

                timeframeCandles.forEach { candle ->
                    currentCandle = currentCandle.copy(
                        high = if (candle.high > currentCandle.high) candle.high else currentCandle.high,
                        low = if (candle.low < currentCandle.low) candle.low else currentCandle.low,
                        close = candle.close,
                    )
                }

                currentCandle
            }
        }

        replayChart.update(
            BarReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        )
    }

    private suspend fun getCandleSeries(
        symbol: String,
        timeframe: Timeframe,
    ): CandleSeries {

        val candleSeriesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = timeframe,
            from = dataFrom,
            to = dataTo,
        )

        return when (candleSeriesResult) {
            is Ok -> candleSeriesResult.value
            is Err -> when (val error = candleSeriesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }
    }

    private fun setInitialData() {

        val currentInstant = baseCandleSeries.list[initialCandleIndex + replayClock.currentOffset].openInstant

        val adjustedIndex = candleSeries.list.indexOfFirst {
            val candleCloseInstant = it.openInstant + (currentTimeframe.seconds - 1).seconds
            currentInstant in it.openInstant..candleCloseInstant
        }

        val data = candleSeries.list.slice(0..adjustedIndex).mapIndexed { index, candle ->
            BarReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        }

        replayChart.setData(data)
    }
}
