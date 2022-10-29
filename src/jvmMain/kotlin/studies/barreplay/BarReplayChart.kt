package studies.barreplay

import androidx.compose.ui.graphics.Color
import chart.IChartApi
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn
import trading.Candle
import java.math.BigDecimal

internal class BarReplayChart(
    private val chart: IChartApi,
) {

    private val candlestickSeries = chart.addCandlestickSeries(
        name = "candlestickSeries",
        options = CandlestickStyleOptions(
            lastValueVisible = false,
        ),
    )

    private val volumeSeries = chart.addHistogramSeries(
        name = "volumeSeries", options = HistogramStyleOptions(
            lastValueVisible = false,
            priceFormat = PriceFormat.BuiltIn(
                type = PriceFormat.Type.Volume,
            ),
            priceScaleId = "",
            priceLineVisible = false,
        )
    )

    private val ema9Series = chart.addLineSeries(
        name = "ema9Series",
        options = LineStyleOptions(
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        ),
    )

    private val vwapSeries = chart.addLineSeries(
        name = "vwapSeries",
        options = LineStyleOptions(
            color = Color.Yellow,
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        ),
    )

    init {

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

    fun setData(dataList: List<Data>) {

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
                value = data.ema9,
            )

            vwapData += LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = data.vwap,
            )
        }

        candlestickSeries.setData(candleData)
        volumeSeries.setData(volumeData)
        ema9Series.setData(ema9Data)
        vwapSeries.setData(vwapData)

        chart.timeScale.scrollToPosition(40, false)
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
                value = data.ema9,
            )
        )

        vwapSeries.update(
            LineData(
                time = Time.UTCTimestamp(offsetTime),
                value = data.vwap,
            )
        )
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
