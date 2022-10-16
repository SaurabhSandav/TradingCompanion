package studies

import AppModule
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import chart.IChartApi
import chart.series.Time
import chart.series.candlestick.CandlestickData
import chart.series.histogram.HistogramData
import chart.series.histogram.HistogramStyleOptions
import chart.series.pricescale.PriceFormat
import chart.series.pricescale.PriceScaleMargins
import chart.series.pricescale.PriceScaleOptions
import chart.series.timescale.TimeScaleOptions
import fyers_api.model.CandleResolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import ui.common.controls.ListSelectionField
import utils.CandleRepo
import utils.NIFTY50

internal class TickerChartStudy(
    private val appModule: AppModule,
    private val candleRepo: CandleRepo = CandleRepo(appModule),
) : ChartStudy() {

    override val name: String = "Ticker Chart"

    var symbol by mutableStateOf<String?>("ICICIBANK")
    var timeframe by mutableStateOf("5m")

    @Composable
    override fun render(chart: @Composable () -> Unit) {

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

            chart()
        }
    }

    override fun CoroutineScope.configureChart(chart: IChartApi) {

        launch {

            snapshotFlow { symbol to timeframe }
                .distinctUntilChanged()
                .filter { (symbol, _) -> symbol != null }
                .collect { (symbol, timeframe) ->

                    val candles = candleRepo.getCandles(
                        symbol = symbol!!,
                        resolution = when (timeframe) {
                            "1D" -> CandleResolution.D1
                            else -> CandleResolution.M5
                        },
                        from = LocalDate(year = 2022, month = Month.JANUARY, dayOfMonth = 1)
                            .atStartOfDayIn(TimeZone.currentSystemDefault()),
                        to = Clock.System.now(),
                    )

                    val candleData = mutableListOf<CandlestickData>()
                    val volumeData = mutableListOf<HistogramData>()

                    candles.forEach { candle ->

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
                    }

                    val candleSeries = chart.addCandlestickSeries()
                    val histogramSeries = chart.addHistogramSeries(
                        HistogramStyleOptions(
                            priceFormat = PriceFormat.BuiltIn(
                                type = PriceFormat.Type.Volume,
                            ),
                            priceScaleId = "",
                        )
                    )

                    histogramSeries.priceScale.applyOptions(
                        PriceScaleOptions(
                            scaleMargins = PriceScaleMargins(
                                top = 0.8,
                                bottom = 0,
                            )
                        )
                    )

                    candleSeries.setData(candleData)
                    histogramSeries.setData(volumeData)

                    chart.timeScale.applyOptions(
                        TimeScaleOptions(timeVisible = true)
                    )
                }
        }
    }
}
