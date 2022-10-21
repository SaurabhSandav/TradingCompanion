package ui.closedtrades.ui

import AppDensityFraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import chart.*
import chart.misc.LineWidth
import chart.misc.PriceFormat
import chart.pricescale.PriceScaleMargins
import chart.pricescale.PriceScaleOptions
import chart.series.candlestick.CandlestickData
import chart.series.data.Time
import chart.series.histogram.HistogramData
import chart.series.histogram.HistogramStyleOptions
import chart.series.line.LineData
import chart.series.line.LineStyleOptions
import chart.timescale.TimeScaleOptions
import fyers_api.model.CandleResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.addclosedtradedetailed.CloseTradeDetailedFormFields
import ui.common.ResizableChart
import utils.CandleRepo

@Composable
internal fun ClosedTradeChartWindow(
    onCloseRequest: () -> Unit,
    candleRepo: CandleRepo,
    formModel: CloseTradeDetailedFormFields.Model,
) {

    Window(
        onCloseRequest = onCloseRequest,
        state = rememberWindowState(placement = WindowPlacement.Maximized),
        title = "Chart",
    ) {

        val density = LocalDensity.current

        val newDensity = Density(density.density * AppDensityFraction, density.fontScale)

        CompositionLocalProvider(LocalDensity provides newDensity) {

            val coroutineScope = rememberCoroutineScope()

            ResizableChart(
                options = ChartOptions(crosshair = CrosshairOptions(mode = CrosshairMode.Normal))
            ) {
                configure(
                    scope = coroutineScope,
                    candleRepo = candleRepo,
                    formModel = formModel,
                )
            }
        }
    }
}

private fun IChartApi.configure(
    scope: CoroutineScope,
    candleRepo: CandleRepo,
    formModel: CloseTradeDetailedFormFields.Model,
) {

    val candleSeries by candlestickSeries()
    val volumeSeries by histogramSeries(
        HistogramStyleOptions(
            priceFormat = PriceFormat.BuiltIn(
                type = PriceFormat.Type.Volume,
            ),
            priceScaleId = "",
        )
    )
    val ema9Series by lineSeries(
        options = LineStyleOptions(
            lineWidth = LineWidth.One,
        ),
    )
    val vwapSeries by lineSeries(
        options = LineStyleOptions(
            lineWidth = LineWidth.One,
            color = Color.Yellow,
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

    timeScale.applyOptions(
        TimeScaleOptions(timeVisible = true)
    )

    scope.launch {

        val from = formModel.entryDateTime.date - DatePeriod(months = 1)
        val to = formModel.exitDateTime.date + DatePeriod(months = 1)

        val candles = candleRepo.getCandles(
            symbol = formModel.ticker!!,
            resolution = CandleResolution.M5,
            from = from.atStartOfDayIn(TimeZone.currentSystemDefault()),
            to = to.atStartOfDayIn(TimeZone.currentSystemDefault()),
        )

        val ema9Indicator = EMAIndicator(ClosePriceIndicator(candles), length = 9)
        val sessionStartTime = LocalTime(hour = 9, minute = 15)
        val vwapIndicator = VWAPIndicator(candles) { candle ->
            candle.openInstant.toLocalDateTime(TimeZone.currentSystemDefault()).time == sessionStartTime
        }

        val candleData = mutableListOf<CandlestickData>()
        val volumeData = mutableListOf<HistogramData>()
        val ema9Data = mutableListOf<LineData>()
        val vwapData = mutableListOf<LineData>()
        var entryIndex = 0
        var exitIndex = 0

        candles.forEachIndexed { index, candle ->

            // Subtract IST Timezone difference
            val epochTime = candle.openInstant.epochSeconds
            val workaroundEpochTime = epochTime + 19800

            candleData += CandlestickData(
                time = Time.UTCTimestamp(workaroundEpochTime),
                open = candle.open,
                high = candle.high,
                low = candle.low,
                close = candle.close,
            )

            volumeData += HistogramData(
                time = Time.UTCTimestamp(workaroundEpochTime),
                value = candle.volume,
                color = when {
                    candle.close < candle.open -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )

            ema9Data += LineData(
                time = Time.UTCTimestamp(workaroundEpochTime),
                value = ema9Indicator[index],
            )

            vwapData += LineData(
                time = Time.UTCTimestamp(workaroundEpochTime),
                value = vwapIndicator[index],
            )

            if (formModel.entryDateTime.toInstant(TimeZone.currentSystemDefault()) > candle.openInstant)
                entryIndex = index
            else if (formModel.exitDateTime.toInstant(TimeZone.currentSystemDefault()) > candle.openInstant)
                exitIndex = index
        }

        candleSeries.setData(candleData)
        volumeSeries.setData(volumeData)
        ema9Series.setData(ema9Data)
        vwapSeries.setData(vwapData)

        timeScale.setVisibleLogicalRange(entryIndex - 20, exitIndex + 20)
    }
}
