package studies

import AppModule
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import chart.IChartApi
import chart.candlestick.CandlestickData
import chart.histogram.HistogramData
import chart.timescale.TimeScaleOptions
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

    var symbol by mutableStateOf<String?>(null)
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

                    candles.forEach {

                        // Subtract IST Timezone difference
                        val epochTime = it.openInstant.epochSeconds
                        val workaroundEpochTime = epochTime + 19800

                        candleData += CandlestickData(
                            time = workaroundEpochTime.toString(),
                            open = it.open.toPlainString(),
                            high = it.high.toPlainString(),
                            low = it.low.toPlainString(),
                            close = it.close.toPlainString(),
                        )

                        volumeData += HistogramData(
                            time = workaroundEpochTime.toString(),
                            value = it.volume,
                        )
                    }

                    val candleSeries = chart.addCandlestickSeries()
                    val histogramSeries = chart.addHistogramSeries()

                    candleSeries.setData(candleData)
                    histogramSeries.setData(volumeData)

                    chart.timeScale.applyOptions(
                        TimeScaleOptions(timeVisible = true)
                    )
                }
        }
    }
}
