package studies.barreplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import chart.IChartApi
import chart.ISeriesApi
import chart.PriceScaleMargins
import chart.PriceScaleOptions
import chart.data.CandlestickData
import chart.data.HistogramData
import chart.data.LineData
import chart.data.Time
import chart.options.CandlestickStyleOptions
import chart.options.HistogramStyleOptions
import chart.options.LineStyleOptions
import chart.options.TimeScaleOptions
import chart.options.common.LineWidth
import chart.options.common.PriceFormat
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.*
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
) {

    private lateinit var chart: IChartApi
    private lateinit var symbol: String
    private lateinit var timeframe: String
    private lateinit var dataFrom: Instant
    private lateinit var dataTo: Instant
    private lateinit var replayFrom: Instant

    private lateinit var candlestickSeries: ISeriesApi<CandlestickData>
    private lateinit var volumeSeries: ISeriesApi<HistogramData>
    private lateinit var ema9Series: ISeriesApi<LineData>
    private lateinit var vwapSeries: ISeriesApi<LineData>

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

    fun setupChart(chart: IChartApi) {

        this.chart = chart

        candlestickSeries = chart.addCandlestickSeries(
            name = "candlestickSeries",
            options = CandlestickStyleOptions(
                lastValueVisible = false,
            ),
        )

        volumeSeries = chart.addHistogramSeries(
            name = "volumeSeries",
            options = HistogramStyleOptions(
                lastValueVisible = false,
                priceFormat = PriceFormat.BuiltIn(
                    type = PriceFormat.Type.Volume,
                ),
                priceScaleId = "",
                priceLineVisible = false,
            )
        )

        ema9Series = chart.addLineSeries(
            name = "ema9Series",
            options = LineStyleOptions(
                lineWidth = LineWidth.One,
                crosshairMarkerVisible = false,
                lastValueVisible = false,
                priceLineVisible = false,
            ),
        )

        vwapSeries = chart.addLineSeries(
            name = "vwapSeries",
            options = LineStyleOptions(
                color = Color.Yellow,
                lineWidth = LineWidth.One,
                crosshairMarkerVisible = false,
                lastValueVisible = false,
                priceLineVisible = false,
            ),
        )

        volumeSeries.priceScale.applyOptions(
            PriceScaleOptions(
                scaleMargins = PriceScaleMargins(
                    top = 0.8,
                    bottom = 0,
                )
            )
        )

        chart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true)
        )
    }

    suspend fun init(
        symbol: String,
        timeframe: String,
        dataFrom: Instant,
        dataTo: Instant,
        replayFrom: Instant,
    ) {

        this.symbol = symbol
        this.timeframe = timeframe
        this.dataFrom = dataFrom
        this.dataTo = dataTo
        this.replayFrom = replayFrom

        val candleSeriesResult = candleRepo.getCandles(
            symbol = symbol,
            timeframe = when (timeframe) {
                "1D" -> Timeframe.D1
                else -> Timeframe.M5
            },
            from = dataFrom,
            to = dataTo,
        )

        candleSeries = when (candleSeriesResult) {
            is Ok -> candleSeriesResult.value
            is Err -> when (val error = candleSeriesResult.error) {
                is CandleRepository.Error.AuthError -> error("AuthError")
                is CandleRepository.Error.UnknownError -> error(error.message)
            }
        }

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

        val candle = candleSeries.list[replayClock.currentIndex]
        val offsetTime = candle.offsetTimeForChart()

        candlestickSeries.update(
            CandlestickData(
                time = Time.UTCTimestamp(offsetTime),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )
        )

        volumeSeries.update(
            HistogramData(
                time = Time.UTCTimestamp(offsetTime),
                value = candle.volume,
                color = when {
                    candle.close < candle.open -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )
        )

        ema9Series.update(
            LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = ema9Indicator[replayClock.currentIndex],
            )
        )

        vwapSeries.update(
            LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = vwapIndicator[replayClock.currentIndex],
            )
        )
    }

    private fun setInitialData() {

        val candleData = mutableListOf<CandlestickData>()
        val volumeData = mutableListOf<HistogramData>()
        val ema9Data = mutableListOf<LineData>()
        val vwapData = mutableListOf<LineData>()

        candleSeries.list.slice(0 until replayClock.currentIndex).forEachIndexed { index, candle ->

            val offsetTime = candle.offsetTimeForChart()

            candleData += CandlestickData(
                time = Time.UTCTimestamp(offsetTime),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )

            volumeData += HistogramData(
                time = Time.UTCTimestamp(offsetTime),
                value = candle.volume,
                color = when {
                    candle.close < candle.open -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )

            ema9Data += LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = ema9Indicator[index],
            )

            vwapData += LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = vwapIndicator[index],
            )
        }

        candlestickSeries.setData(candleData)
        volumeSeries.setData(volumeData)
        ema9Series.setData(ema9Data)
        vwapSeries.setData(vwapData)

        chart.timeScale.scrollToPosition(40, false)
    }

    private fun Candle.offsetTimeForChart(): Long {
        val epochTime = openInstant.epochSeconds
        val timeZoneOffset = openInstant.offsetIn(TimeZone.currentSystemDefault()).totalSeconds
        return epochTime + timeZoneOffset
    }
}
