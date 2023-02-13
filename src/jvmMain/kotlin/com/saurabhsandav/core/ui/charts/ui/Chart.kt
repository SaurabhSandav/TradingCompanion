package com.saurabhsandav.core.ui.charts.ui

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.chart.*
import com.saurabhsandav.core.chart.callbacks.TimeRangeChangeEventHandler
import com.saurabhsandav.core.chart.data.CandlestickData
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.chart.misc.TimeRange
import com.saurabhsandav.core.chart.options.CandlestickStyleOptions
import com.saurabhsandav.core.chart.options.HistogramStyleOptions
import com.saurabhsandav.core.chart.options.LineStyleOptions
import com.saurabhsandav.core.chart.options.TimeScaleOptions
import com.saurabhsandav.core.chart.options.common.LineWidth
import com.saurabhsandav.core.chart.options.common.PriceFormat
import com.saurabhsandav.core.trading.Candle
import com.saurabhsandav.core.ui.common.chart.ChartDarkModeOptions
import com.saurabhsandav.core.ui.common.chart.ChartLightModeOptions
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn
import java.math.BigDecimal
import java.math.RoundingMode

internal class Chart(
    coroutineScope: CoroutineScope,
    val actualChart: IChartApi,
    onLoadMore: suspend () -> Unit,
) {

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

    private fun legendValuesFlow(): Flow<List<String>> = actualChart.crosshairMove().map { params ->

        val candlestickSeriesPrices = params.getSeriesPrices(candlestickSeries)
        val open = candlestickSeriesPrices?.open?.toString().orEmpty()
        val high = candlestickSeriesPrices?.high?.toString().orEmpty()
        val low = candlestickSeriesPrices?.low?.toString().orEmpty()
        val close = candlestickSeriesPrices?.close?.toString().orEmpty()
        val volume = volumeSeries?.let { params.getSeriesPrice(it)?.value?.toString() }.orEmpty()
        val ema9 = params.getSeriesPrice(ema9Series)?.value?.toString().orEmpty()
        val vwap = vwapSeries?.let { params.getSeriesPrice(it)?.value?.toString() }.orEmpty()

        listOf(
            "O $open H $high L $low C $close",
            "Vol $volume",
            "VWAP $vwap",
            "EMA (9) $ema9",
        )
    }

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
