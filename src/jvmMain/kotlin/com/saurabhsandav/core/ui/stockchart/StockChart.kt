package com.saurabhsandav.core.ui.stockchart

import androidx.compose.ui.graphics.Color
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.AppModule
import com.saurabhsandav.core.chart.IChartApi
import com.saurabhsandav.core.chart.data.HistogramData
import com.saurabhsandav.core.chart.data.LineData
import com.saurabhsandav.core.chart.data.Time
import com.saurabhsandav.core.chart.options.TimeScaleOptions
import com.saurabhsandav.core.trading.CandleSeries
import com.saurabhsandav.core.trading.dailySessionStart
import com.saurabhsandav.core.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.core.trading.indicator.EMAIndicator
import com.saurabhsandav.core.trading.indicator.VWAPIndicator
import com.saurabhsandav.core.trading.isLong
import com.saurabhsandav.core.ui.common.chart.ChartDarkModeOptions
import com.saurabhsandav.core.ui.common.chart.ChartLightModeOptions
import com.saurabhsandav.core.ui.common.chart.crosshairMove
import com.saurabhsandav.core.ui.stockchart.plotter.LinePlotter
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.VolumePlotter
import com.saurabhsandav.core.utils.PrefDefaults
import com.saurabhsandav.core.utils.PrefKeys
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetIn
import java.math.RoundingMode

internal class StockChart(
    val appModule: AppModule,
    val actualChart: IChartApi,
    appPrefs: FlowSettings = appModule.appPrefs,
    onLegendUpdate: (List<String>) -> Unit,
) {

    private val coroutineScope = MainScope()
    private val plotters = mutableSetOf<SeriesPlotter<*>>()
    private var source: CandleSource? = null

    init {

        actualChart.timeScale.applyOptions(
            TimeScaleOptions(timeVisible = true)
        )

        // Setting dark mode according to settings
        coroutineScope.launch {
            appPrefs.getBooleanFlow(PrefKeys.DarkModeEnabled, PrefDefaults.DarkModeEnabled).collect { isDark ->
                actualChart.applyOptions(if (isDark) ChartDarkModeOptions else ChartLightModeOptions)
            }
        }

        actualChart.crosshairMove().onEach { params ->
            onLegendUpdate(plotters.map { it.legendText(params) })
        }.launchIn(coroutineScope)
    }

    fun setCandleSource(source: CandleSource) {

        plotters.forEach { it.remove() }
        plotters.clear()
        this.source?.coroutineScope?.cancel()
        this.source = source

        val candlestickPlotter = source.init(actualChart)

        source.coroutineScope.launch {
            source.candleSeries.live.collect { candle ->
                plotters.forEach {
                    it.update(source.candleSeries.indexOf(candle))
                }
            }
        }

        plotters.add(candlestickPlotter)
        setupDefaultIndicators(source.candleSeries, source.hasVolume)

        plotters.forEach { it.setData(source.candleSeries.indices) }
    }

    private fun setupDefaultIndicators(
        candleSeries: CandleSeries,
        hasVolume: Boolean,
    ) {

        val ema9Indicator = EMAIndicator(ClosePriceIndicator(candleSeries), length = 9)
        val vwapIndicator = VWAPIndicator(candleSeries, ::dailySessionStart)

        LinePlotter(actualChart, "EMA (9)") { index ->
            LineData(
                time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                value = ema9Indicator[index].setScale(2, RoundingMode.DOWN),
            )
        }.also(plotters::add)

        if (!hasVolume) return

        VolumePlotter(actualChart) { index ->

            val candle = candleSeries[index]

            HistogramData(
                time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart()),
                value = candle.volume,
                color = when {
                    candle.isLong -> Color(255, 82, 82)
                    else -> Color(0, 150, 136)
                },
            )
        }.also(plotters::add)

        LinePlotter(actualChart, "VWAP") { index ->
            LineData(
                time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                value = vwapIndicator[index].setScale(2, RoundingMode.DOWN),
            )
        }.also(plotters::add)
    }

    fun destroy() {
        coroutineScope.cancel()
        source?.coroutineScope?.cancel()
        plotters.forEach { it.remove() }
        actualChart.remove()
    }
}

fun Instant.offsetTimeForChart(): Long {
    val timeZoneOffset = offsetIn(TimeZone.currentSystemDefault()).totalSeconds
    return epochSeconds + timeZoneOffset
}
