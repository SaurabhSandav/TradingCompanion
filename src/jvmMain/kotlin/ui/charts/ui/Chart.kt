package ui.charts.ui

import androidx.compose.ui.graphics.Color
import chart.*
import chart.callbacks.MouseEventHandler
import chart.callbacks.TimeRangeChangeEventHandler
import chart.data.CandlestickData
import chart.data.HistogramData
import chart.data.LineData
import chart.data.Time
import chart.misc.TimeRange
import chart.options.*
import chart.options.common.LineWidth
import chart.options.common.PriceFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn
import trading.Candle
import ui.charts.model.ChartsState.LegendValues
import ui.common.chart.ChartDarkModeOptions
import ui.common.chart.ChartLightModeOptions
import java.math.BigDecimal
import java.math.RoundingMode

internal class Chart(
    coroutineScope: CoroutineScope,
    container: String,
    name: String,
    onLoadMore: suspend () -> Unit,
) {

    val actualChart = createChart(
        container = container,
        name = name,
        options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)),
    )
    val legendValues = legendValuesFlow()
    val visibleTimeRange = visibleTimeRangeFlow()

    private val candlestickSeries by actualChart.candlestickSeries(
        options = CandlestickStyleOptions(
            lastValueVisible = false,
        ),
    )

    private val ema9Series by actualChart.lineSeries(
        options = LineStyleOptions(
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        ),
    )

    private var volumeSeries: ISeriesApi<HistogramData>? = null

    private var vwapSeries: ISeriesApi<LineData>? = null

    private var moreCandlesJob: Job? = null

    init {

        actualChart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true)
        )

        actualChart.timeScale.subscribeVisibleLogicalRangeChange { logicalRange ->

            if (moreCandlesJob != null || logicalRange == null) return@subscribeVisibleLogicalRangeChange

            moreCandlesJob = coroutineScope.launch {

                val barsInfo = candlestickSeries.barsInLogicalRange(logicalRange)

                // Load more historical data if there are less than 100 bars to the left of the visible area
                if (barsInfo != null && barsInfo.barsBefore < 100) {
                    onLoadMore()
                }

                moreCandlesJob = null
            }
        }

        // Initially, leave some empty space to the right of the candles
        actualChart.timeScale.scrollToPosition(40, false)
    }

    fun setData(dataList: List<Data>, hasVolume: Boolean) {

        when {
            hasVolume -> enableVolume()
            else -> disableVolume()
        }

        val candleData = mutableListOf<CandlestickData>()
        val volumeData = mutableListOf<HistogramData>()
        val ema9Data = mutableListOf<LineData>()
        val vwapData = mutableListOf<LineData>()

        dataList.forEach { data ->

            val candle = data.candle
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
                value = data.ema9.setScale(2, RoundingMode.DOWN),
            )

            vwapData += LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = data.vwap.setScale(2, RoundingMode.DOWN),
            )
        }

        candlestickSeries.setData(candleData)
        volumeSeries?.setData(volumeData)
        ema9Series.setData(ema9Data)
        vwapSeries?.setData(vwapData)
    }

    fun update(data: Data) {

        val candle = data.candle
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

        volumeSeries?.update(
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
                value = data.ema9.setScale(2, RoundingMode.DOWN),
            )
        )

        vwapSeries?.update(
            LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = data.vwap.setScale(2, RoundingMode.DOWN),
            )
        )
    }

    fun setDarkMode(isDark: Boolean) {
        actualChart.applyOptions(if (isDark) ChartDarkModeOptions else ChartLightModeOptions)
    }

    fun setVisibleRange(range: TimeRange) {
        actualChart.timeScale.setVisibleRange(range.from, range.to)
    }

    private fun legendValuesFlow(): Flow<LegendValues> = callbackFlow {

        val handler = MouseEventHandler { params ->

            val candlestickSeriesPrices = params.getSeriesPrices(candlestickSeries)
            val open = candlestickSeriesPrices?.open?.toPlainString().orEmpty()
            val high = candlestickSeriesPrices?.high?.toPlainString().orEmpty()
            val low = candlestickSeriesPrices?.low?.toPlainString().orEmpty()
            val close = candlestickSeriesPrices?.close?.toPlainString().orEmpty()
            val volume = volumeSeries?.let { params.getSeriesPrice(it)?.value?.toPlainString() }.orEmpty()
            val ema9 = params.getSeriesPrice(ema9Series)?.value?.toPlainString().orEmpty()
            val vwap = vwapSeries?.let { params.getSeriesPrice(it)?.value?.toPlainString() }.orEmpty()

            trySend(
                LegendValues(
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    ema9 = ema9,
                    vwap = vwap,
                )
            )
        }

        actualChart.subscribeCrosshairMove(handler)

        awaitClose { actualChart.unsubscribeCrosshairMove(handler) }
    }.buffer(Channel.CONFLATED)

    private fun visibleTimeRangeFlow(): Flow<TimeRange?> = callbackFlow {

        val handler = TimeRangeChangeEventHandler { range -> trySend(range) }

        actualChart.timeScale.subscribeVisibleTimeRangeChange(handler)

        awaitClose { actualChart.timeScale.unsubscribeVisibleTimeRangeChange(handler) }
    }.buffer(Channel.CONFLATED)

    private fun enableVolume() {

        if (volumeSeries != null) return

        volumeSeries = actualChart.addHistogramSeries(
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

        vwapSeries = actualChart.addLineSeries(
            name = "vwapSeries",
            options = LineStyleOptions(
                color = Color.Yellow,
                lineWidth = LineWidth.One,
                crosshairMarkerVisible = false,
                lastValueVisible = false,
                priceLineVisible = false,
            ),
        )

        volumeSeries!!.priceScale.applyOptions(
            PriceScaleOptions(
                scaleMargins = PriceScaleMargins(
                    top = 0.8,
                    bottom = 0,
                )
            )
        )
    }

    private fun disableVolume() {

        if (volumeSeries == null) return

        actualChart.removeSeries(volumeSeries!!)
        actualChart.removeSeries(vwapSeries!!)

        volumeSeries = null
        vwapSeries = null
    }

    private fun Candle.offsetTimeForChart(): Long {
        val epochTime = openInstant.epochSeconds
        val timeZoneOffset = openInstant.offsetIn(TimeZone.currentSystemDefault()).totalSeconds
        return epochTime + timeZoneOffset
    }

    data class Data(
        val candle: Candle,
        val ema9: BigDecimal,
        val vwap: BigDecimal,
    )
}
