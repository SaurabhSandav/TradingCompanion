package com.saurabhsandav.core.ui.stockchart

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import com.russhwolf.settings.coroutines.FlowSettings
import com.saurabhsandav.core.ui.common.chart.offsetTimeForChart
import com.saurabhsandav.core.ui.common.hex
import com.saurabhsandav.core.ui.common.toCssColor
import com.saurabhsandav.core.ui.stockchart.data.StockChartData
import com.saurabhsandav.core.ui.stockchart.plotter.CandlestickPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.LinePlotter
import com.saurabhsandav.core.ui.stockchart.plotter.SeriesPlotter
import com.saurabhsandav.core.ui.stockchart.plotter.VolumePlotter
import com.saurabhsandav.core.utils.launchUnit
import com.saurabhsandav.core.utils.mapList
import com.saurabhsandav.kbigdecimal.KRoundingMode
import com.saurabhsandav.kbigdecimal.toDouble
import com.saurabhsandav.lightweightcharts.data.CandlestickData
import com.saurabhsandav.lightweightcharts.data.HistogramData
import com.saurabhsandav.lightweightcharts.data.LineData
import com.saurabhsandav.lightweightcharts.data.Time
import com.saurabhsandav.lightweightcharts.plugin.SessionMarkers
import com.saurabhsandav.lightweightcharts.plugin.TradeExecutionMarkers
import com.saurabhsandav.lightweightcharts.plugin.TradeMarkers
import com.saurabhsandav.trading.core.Candle
import com.saurabhsandav.trading.core.CandleSeries
import com.saurabhsandav.trading.core.SessionChecker
import com.saurabhsandav.trading.core.Timeframe
import com.saurabhsandav.trading.core.isLong
import com.saurabhsandav.trading.indicator.ClosePriceIndicator
import com.saurabhsandav.trading.indicator.EMAIndicator
import com.saurabhsandav.trading.indicator.SMAIndicator
import com.saurabhsandav.trading.indicator.VWAPIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlin.time.Instant

class PlotterManager(
    private val coroutineScope: CoroutineScope,
    private val stockChart: StockChart,
    private val prefs: FlowSettings,
) {

    private var indicators: Indicators? = null

    internal val candlestickPlotter = CandlestickPlotter("candles")
    private val volumePlotter = VolumePlotter("volume")
    private val vwapPlotter = LinePlotter("vwap", "VWAP", Color.hex("#FFA500"))
    private val ema9Plotter = LinePlotter("ema9", "EMA (9)", Color.hex("#2962FF"))
    private val ema21Plotter = LinePlotter("ema21", "EMA (21)", Color.hex("#F7525F"))
    private val sma50Plotter = LinePlotter("sma50", "SMA (50)", Color.hex("#0AB210"))
    private val sma100Plotter = LinePlotter("sma100", "SMA (100)", Color.hex("#B05F10"))
    private val sma200Plotter = LinePlotter("sma200", "SMA (200)", Color.hex("#B00C10"))

    private val sessionMarkers = SessionMarkers()
    private val tradeExecutionMarkers = TradeExecutionMarkers()
    private val tradeMarkers = TradeMarkers()

    val areMarkersEnabled = prefs.getBooleanFlow(PrefMarkersEnabled, false)

    val plotters = mutableStateListOf<Plotter<*>>()

    init {

        plotters.addAll(
            listOf(
                candlestickPlotter,
                volumePlotter,
                vwapPlotter,
                ema9Plotter,
                ema21Plotter,
                sma50Plotter,
                sma100Plotter,
                sma200Plotter,
            ),
        )

        plotters.forEach { plotter -> plotter.onAttach(stockChart) }

        candlestickPlotter.series.attachPrimitive(sessionMarkers)
        candlestickPlotter.series.attachPrimitive(tradeExecutionMarkers)
        candlestickPlotter.series.attachPrimitive(tradeMarkers)

        // Observe plotter enabled prefs
        plotters.forEach { plotter ->
            prefs
                .getBooleanFlow(plotter.prefKey, true)
                .onEach { plotter.isEnabled = it }
                .launchIn(coroutineScope)
        }
    }

    internal fun onSetStockChartData(
        data: StockChartData,
        hasVolume: Boolean,
        sessionChecker: SessionChecker,
        dataCoroutineScope: CoroutineScope,
    ) {

        indicators = Indicators(
            candleSeries = data.candleSeries,
            params = data.params,
            hasVolume = hasVolume,
            sessionChecker = sessionChecker,
        )

        val candleSeries = data.candleSeries

        // Set initial data
        setData()

        // Set latest values to legend
        plotters.forEach { plotter ->
            if (plotter is SeriesPlotter<*, *>) plotter.updateLegendValues(null)
        }

        // Session markers
        generateSessionStartInstants(sessionChecker, candleSeries)
            .onEach { instants ->
                val times = instants.map { Time.UTCTimestamp(it.offsetTimeForChart()) }
                sessionMarkers.setTimes(times)
            }
            .launchIn(dataCoroutineScope)

        // Trade execution markers
        combine(areMarkersEnabled, data.tradeExecutionMarkers) { markersEnabled, tradeExecutionMarkers ->
            if (markersEnabled) tradeExecutionMarkers else emptyList()
        }.mapList { it.toActualMarker(candleSeries) }
            .onEach(tradeExecutionMarkers::setExecutions)
            .launchIn(dataCoroutineScope)

        // Trade markers
        combine(areMarkersEnabled, data.tradeMarkers) { markersEnabled, tradeMarkers ->
            if (markersEnabled) tradeMarkers else emptyList()
        }.mapList { it.toActualMarker(candleSeries) }
            .onEach(tradeMarkers::setTrades)
            .launchIn(dataCoroutineScope)
    }

    internal fun setData() {

        val indicators = indicators ?: return
        val candleSeries = indicators.candleSeries

        candlestickPlotter.setData(
            candleSeries.map { candle ->

                CandlestickData.Item(
                    time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart()),
                    open = candle.open.toDouble(),
                    high = candle.high.toDouble(),
                    low = candle.low.toDouble(),
                    close = candle.close.toDouble(),
                )
            },
        )

        ema9Plotter.setData(
            candleSeries.indices.map { index ->
                LineData.Item(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = indicators.ema9Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                )
            },
        )

        ema21Plotter.setData(
            candleSeries.indices.map { index ->
                LineData.Item(
                    time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                    value = indicators.ema21Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                )
            },
        )

        if (indicators.hasVolume) {

            volumePlotter.setData(
                candleSeries.indices.map { index ->

                    val candle = candleSeries[index]

                    HistogramData.Item(
                        time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart()),
                        value = candle.volume.toDouble(),
                        color = Color.hex(if (candle.isLong) "#009688" else "#FF5252")?.toCssColor(),
                    )
                },
            )
        } else {
            volumePlotter.setData(emptyList())
        }

        val vwapIndicator = indicators.vwapIndicator

        if (vwapIndicator != null) {

            vwapPlotter.setData(
                candleSeries.indices.map { index ->
                    LineData.Item(
                        time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                        value = vwapIndicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                    )
                },
            )
        } else {
            vwapPlotter.setData(emptyList())
        }

        val sma50Indicator = indicators.sma50Indicator

        if (sma50Indicator != null) {

            sma50Plotter.setData(
                candleSeries.indices.map { index ->
                    LineData.Item(
                        time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                        value = sma50Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                    )
                },
            )
        } else {
            sma50Plotter.setData(emptyList())
        }

        val sma100Indicator = indicators.sma100Indicator

        if (sma100Indicator != null) {

            sma100Plotter.setData(
                candleSeries.indices.map { index ->
                    LineData.Item(
                        time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                        value = sma100Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                    )
                },
            )
        } else {
            sma100Plotter.setData(emptyList())
        }

        val sma200Indicator = indicators.sma200Indicator

        if (sma200Indicator != null) {

            sma200Plotter.setData(
                candleSeries.indices.map { index ->
                    LineData.Item(
                        time = Time.UTCTimestamp(candleSeries[index].openInstant.offsetTimeForChart()),
                        value = sma200Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                    )
                },
            )
        } else {
            sma200Plotter.setData(emptyList())
        }
    }

    internal fun update(
        index: Int,
        candle: Candle,
    ) {

        val indicators = requireNotNull(indicators)
        val time = Time.UTCTimestamp(candle.openInstant.offsetTimeForChart())

        candlestickPlotter.update(
            CandlestickData.Item(
                time = time,
                open = candle.open.toDouble(),
                high = candle.high.toDouble(),
                low = candle.low.toDouble(),
                close = candle.close.toDouble(),
            ),
        )

        ema9Plotter.update(
            LineData.Item(
                time = time,
                value = indicators.ema9Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
            ),
        )

        ema21Plotter.update(
            LineData.Item(
                time = time,
                value = indicators.ema21Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
            ),
        )

        if (indicators.hasVolume) {

            volumePlotter.update(
                HistogramData.Item(
                    time = time,
                    value = candle.volume.toDouble(),
                    color = Color.hex(if (candle.isLong) "#009688" else "#FF5252")?.toCssColor(),
                ),
            )
        }

        val vwapIndicator = indicators.vwapIndicator

        if (vwapIndicator != null) {

            vwapPlotter.update(
                LineData.Item(
                    time = time,
                    value = vwapIndicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                ),
            )
        }

        val sma50Indicator = indicators.sma50Indicator

        if (sma50Indicator != null) {

            sma50Plotter.update(
                LineData.Item(
                    time = time,
                    value = sma50Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                ),
            )
        }

        val sma100Indicator = indicators.sma100Indicator

        if (sma100Indicator != null) {

            sma100Plotter.update(
                LineData.Item(
                    time = time,
                    value = sma100Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                ),
            )
        }

        val sma200Indicator = indicators.sma200Indicator

        if (sma200Indicator != null) {

            sma200Plotter.update(
                LineData.Item(
                    time = time,
                    value = sma200Indicator[index].decimalPlaces(2, KRoundingMode.Down).toDouble(),
                ),
            )
        }
    }

    fun setPlotterEnabled(
        plotter: Plotter<*>,
        isEnabled: Boolean,
    ) = coroutineScope.launchUnit {
        prefs.putBoolean(plotter.prefKey, isEnabled)
    }

    fun setMarkersEnabled(isEnabled: Boolean) = coroutineScope.launchUnit {
        prefs.putBoolean(PrefMarkersEnabled, isEnabled)
    }

    private fun generateSessionStartInstants(
        sessionChecker: SessionChecker,
        candleSeries: CandleSeries,
    ): Flow<List<Instant>> {
        return candleSeries.instantRange.map {
            candleSeries
                .filterIndexed { index, _ -> sessionChecker.isSessionStart(candleSeries, index) }
                .map { it.openInstant }
        }
    }

    private class Indicators(
        val candleSeries: CandleSeries,
        val params: StockChartParams,
        val hasVolume: Boolean,
        sessionChecker: SessionChecker,
    ) {

        private val isDaily = params.timeframe == Timeframe.D1

        val closePriceIndicator = ClosePriceIndicator(candleSeries)
        val ema9Indicator = EMAIndicator(closePriceIndicator, length = 9)
        val ema21Indicator = EMAIndicator(closePriceIndicator, length = 21)
        val vwapIndicator = VWAPIndicator(candleSeries, sessionChecker).takeIf { hasVolume }

        val sma50Indicator = SMAIndicator(closePriceIndicator, length = 50).takeIf { isDaily }
        val sma100Indicator = SMAIndicator(closePriceIndicator, length = 100).takeIf { isDaily }
        val sma200Indicator = SMAIndicator(closePriceIndicator, length = 200).takeIf { isDaily }
    }
}

private val Plotter<*>.prefKey
    get() = "plotter_${key}_enabled"

private const val PrefMarkersEnabled = "markers_enabled"
