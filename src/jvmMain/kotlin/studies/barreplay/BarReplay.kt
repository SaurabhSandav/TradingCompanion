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
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import trading.Candle
import trading.CandleSeries
import trading.Timeframe
import trading.data.CandleRepository
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

    private val sessionStartTime = LocalTime(hour = 9, minute = 15)
    private var currentSymbol = symbol
    private var currentTimeframe = timeframe

    private lateinit var candleSeries: CandleSeries
    private lateinit var ema9Indicator: EMAIndicator
    private lateinit var vwapIndicator: VWAPIndicator

    private val isSessionStart: (Candle) -> Boolean = { candle ->
        candle.openInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time == sessionStartTime
    }

    private lateinit var replayClock: BarReplayClock

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

        val initialCandleIndex = candleSeries.list.indexOfFirst { it.openInstant >= replayFrom }

        replayClock = BarReplayClock(
            initialTime = candleSeries.list[initialCandleIndex].openInstant,
            initialIndex = initialCandleIndex,
            timeframe = timeframe,
        )

        ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        vwapIndicator = VWAPIndicator(candleSeries, isSessionStart)

        setInitialData()
    }

    fun newSymbol(symbol: String) = coroutineScope.launch {

        currentSymbol = symbol

        candleSeries = getCandleSeries(symbol, currentTimeframe)
        ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        vwapIndicator = VWAPIndicator(candleSeries, isSessionStart)

        setInitialData()
    }

    fun newTimeframe(timeframe: Timeframe) = coroutineScope.launch {

        if (timeframe.seconds < this@BarReplay.timeframe.seconds) error("New Timeframe cannot be less than initial Timeframe")

        currentTimeframe = timeframe

        candleSeries = getCandleSeries(symbol, timeframe)
        ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        vwapIndicator = VWAPIndicator(candleSeries, isSessionStart)

        setInitialData()
    }

    fun reset() {

        replayClock.reset()
        isAutoNextEnabled = false

        setInitialData()
    }

    fun next() {

        replayClock.next()

        val adjustedIndex = candleSeries.list.indexOfFirst {
            replayClock.currentTime in it.openInstant..(it.openInstant + currentTimeframe.seconds.seconds)
        }
        val candle = candleSeries.list[adjustedIndex]

        replayChart.update(
            BarReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[adjustedIndex],
                vwap = vwapIndicator[adjustedIndex],
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

    private fun CandleSeries.resampled(timeframe: Timeframe): CandleSeries {

        val candleSeries = CandleSeries()

        val initialCandleIndex = candleSeries.list.indexOfFirst(isSessionStart)
        val timeframeDuration = timeframe.seconds.seconds
        var currentCandle = list[initialCandleIndex]

        list.slice(initialCandleIndex..list.lastIndex).forEach { candle ->

            if ((currentCandle.openInstant + timeframeDuration) == candle.openInstant) {
                candleSeries.addCandle(candle)
                currentCandle = candle
                return@forEach
            }

            currentCandle = currentCandle.copy(
                high = if (candle.high > currentCandle.high) candle.high else currentCandle.high,
                low = if (candle.low < currentCandle.low) candle.low else currentCandle.low,
                close = candle.close,
            )
        }

        return candleSeries
    }

    private fun setInitialData() {

        val adjustedIndex = candleSeries.list.indexOfFirst {
            replayClock.currentTime in it.openInstant..(it.openInstant + currentTimeframe.seconds.seconds)
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
