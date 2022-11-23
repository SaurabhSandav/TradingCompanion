package ui.barreplay.charts.ui

import androidx.compose.ui.graphics.Color
import chart.*
import chart.data.*
import chart.options.*
import chart.options.common.LineWidth
import chart.options.common.PriceFormat
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn
import trading.Candle
import ui.common.ChartDarkModeOptions
import ui.common.ChartLightModeOptions
import ui.common.ChartState
import utils.PrefDefaults
import utils.PrefKeys
import java.math.BigDecimal
import java.math.RoundingMode

internal class ReplayChart(
    coroutineScope: CoroutineScope,
    appPrefs: FlowSettings,
    onDataUpdate: (List<Pair<String, String>>) -> Unit,
) {

    private val chart = createChart(ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
    val chartState = ChartState(coroutineScope, chart)

    private val candlestickSeries by chart.candlestickSeries(
        options = CandlestickStyleOptions(
            lastValueVisible = false,
        ),
    )

    private val ema9Series by chart.lineSeries(
        options = LineStyleOptions(
            lineWidth = LineWidth.One,
            crosshairMarkerVisible = false,
            lastValueVisible = false,
            priceLineVisible = false,
        ),
    )

    private var volumeSeries: ISeriesApi<HistogramData>? = null

    private var vwapSeries: ISeriesApi<LineData>? = null

    init {

        coroutineScope.launch {
            appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled).collect { isDark ->
                chart.applyOptions(if (isDark) ChartDarkModeOptions else ChartLightModeOptions)
            }
        }

        chart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true)
        )

        chart.subscribeCrosshairMove { params ->

            val displayData = when {
                params.seriesPrices.isEmpty() -> getDataList()
                else -> getDataList(
                    price = (params.seriesPrices[candlestickSeries] as GeneralData.BarPrices).close,
                    volume = volumeSeries?.let { (params.seriesPrices[it] as GeneralData.BarPrice).value },
                    ema9 = (params.seriesPrices[ema9Series] as GeneralData.BarPrice).value,
                    vwap = vwapSeries?.let { (params.seriesPrices[it] as GeneralData.BarPrice).value },
                )
            }

            onDataUpdate(displayData)
        }
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

    private fun enableVolume() {

        if (volumeSeries != null) return

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

        chart.removeSeries(volumeSeries!!)
        chart.removeSeries(vwapSeries!!)

        volumeSeries = null
        vwapSeries = null
    }

    private fun Candle.offsetTimeForChart(): Long {
        val epochTime = openInstant.epochSeconds
        val timeZoneOffset = openInstant.offsetIn(TimeZone.currentSystemDefault()).totalSeconds
        return epochTime + timeZoneOffset
    }

    private fun getDataList(
        price: BigDecimal? = null,
        volume: BigDecimal? = null,
        ema9: BigDecimal? = null,
        vwap: BigDecimal? = null,
    ): List<Pair<String, String>> = buildList {
        add("Price" to price?.toPlainString().orEmpty())
        add("Volume" to volume?.toPlainString().orEmpty())
        add("EMA (9)" to ema9?.toPlainString().orEmpty())
        add("VWAP" to vwap?.toPlainString().orEmpty())
    }

    data class Data(
        val candle: Candle,
        val ema9: BigDecimal,
        val vwap: BigDecimal,
    )
}
