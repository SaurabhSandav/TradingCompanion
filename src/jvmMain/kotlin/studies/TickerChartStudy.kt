package studies

import AppModule
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chart.*
import chart.data.CandlestickData
import chart.data.HistogramData
import chart.data.LineData
import chart.data.Time
import chart.options.*
import chart.options.common.LineWidth
import chart.options.common.PriceFormat
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import trading.Timeframe
import trading.dailySessionStart
import trading.data.CandleRepository
import trading.indicator.ClosePriceIndicator
import trading.indicator.EMAIndicator
import trading.indicator.VWAPIndicator
import ui.common.ResizableChart
import ui.common.controls.ListSelectionField
import ui.common.rememberChartState
import ui.common.themedChartOptions
import utils.NIFTY50

internal class TickerChartStudy(
    private val appModule: AppModule,
    private val candleRepo: CandleRepository = CandleRepository(appModule),
) : Study {

    var symbol by mutableStateOf<String?>("ICICIBANK")
    var timeframe by mutableStateOf("5m")

    @Composable
    override fun render() {

        Column {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {

                ListSelectionField(
                    items = NIFTY50,
                    onSelection = { symbol = it },
                    selection = symbol,
                )

                ListSelectionField(
                    items = listOf("5m", "1D"),
                    onSelection = { timeframe = it },
                    selection = timeframe,
                )
            }

            val themedOptions = themedChartOptions()

            val chart = remember {
                createChart(themedOptions.copy(crosshair = CrosshairOptions(mode = CrosshairMode.Normal)))
            }

            ResizableChart(rememberChartState(chart))

            LaunchedEffect(chart) {
                chart.configure(this)
            }

            LaunchedEffect(themedOptions) {
                chart.applyOptions(themedOptions)
            }
        }
    }

    private fun IChartApi.configure(scope: CoroutineScope) {

        scope.launch {

            snapshotFlow { symbol to timeframe }
                .distinctUntilChanged()
                .filter { (symbol, _) -> symbol != null }
                .collect { (symbol, timeframe) ->

                    val candleSeriesResult = candleRepo.getCandles(
                        symbol = symbol!!,
                        timeframe = when (timeframe) {
                            "1D" -> Timeframe.D1
                            else -> Timeframe.M5
                        },
                        from = LocalDate(year = 2022, month = Month.OCTOBER, dayOfMonth = 1)
                            .atStartOfDayIn(TimeZone.currentSystemDefault()),
                        to = Clock.System.now(),
                    )

                    val candles = when (candleSeriesResult) {
                        is Ok -> candleSeriesResult.value
                        is Err -> when (val error = candleSeriesResult.error) {
                            is CandleRepository.Error.AuthError -> error("AuthError")
                            is CandleRepository.Error.UnknownError -> error(error.message)
                        }
                    }

                    val ema9Indicator = EMAIndicator(ClosePriceIndicator(candles), length = 9)
                    val vwapIndicator = VWAPIndicator(candles, ::dailySessionStart)

                    val candleData = mutableListOf<CandlestickData>()
                    val volumeData = mutableListOf<HistogramData>()
                    val ema9Data = mutableListOf<LineData>()
                    val vwapData = mutableListOf<LineData>()

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
                    }

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

                    candleSeries.setData(candleData)
                    volumeSeries.setData(volumeData)
                    ema9Series.setData(ema9Data)
                    vwapSeries.setData(vwapData)

                    timeScale.applyOptions(
                        TimeScaleOptions(timeVisible = true)
                    )
                }
        }
    }

    class Factory(private val appModule: AppModule) : Study.Factory<TickerChartStudy> {

        override val name: String = "Ticker Chart"

        override fun create() = TickerChartStudy(appModule)
    }
}
