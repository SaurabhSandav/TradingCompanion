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
    coroutineScope: CoroutineScope,
    private val replayChart: BarReplayChart,
    private val symbol: String,
    private val timeframe: String,
    private val dataFrom: Instant,
    private val dataTo: Instant,
    private val replayFrom: Instant,
) {

    private val sessionStartTime = LocalTime(hour = 9, minute = 15)

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

        candleSeries = getCandleSeries()

        val initialCandleIndex = candleSeries.list.indexOfFirst { it.openInstant >= replayFrom }

        replayClock = BarReplayClock(
            initialTime = candleSeries.list[initialCandleIndex].openInstant,
            initialIndex = initialCandleIndex,
            timeframe = when (timeframe) {
                "1D" -> Timeframe.D1
                else -> Timeframe.M5
            },
        )

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

        val currentIndex = replayClock.currentIndex
        val candle = candleSeries.list[currentIndex]

        replayChart.update(
            BarReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[currentIndex],
                vwap = vwapIndicator[currentIndex],
            )
        )
    }

    private suspend fun getCandleSeries(): CandleSeries {

        val candleSeriesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = when (timeframe) {
                "1D" -> Timeframe.D1
                else -> Timeframe.M5
            },
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

        val data = candleSeries.list.slice(0 until replayClock.currentIndex).mapIndexed { index, candle ->
            BarReplayChart.Data(
                candle = candle,
                ema9 = ema9Indicator[index],
                vwap = vwapIndicator[index],
            )
        }

        replayChart.setData(data)
    }
}
