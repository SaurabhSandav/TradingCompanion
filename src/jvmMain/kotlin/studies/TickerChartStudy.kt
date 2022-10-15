package studies

import AppModule
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import chart.IChartApi
import chart.ISeriesApi
import chart.candlestick.CandlestickData
import chart.histogram.HistogramData
import chart.timescale.TimeScaleOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import ui.common.controls.ListSelectionField
import utils.NIFTY50
import kotlin.io.path.Path
import kotlin.io.path.readText

internal class TickerChartStudy(
    private val appModule: AppModule,
) : ChartStudy() {

    override val name: String = "Ticker Chart"

    var symbol by mutableStateOf<String?>(null)

    @Composable
    override fun render(chart: @Composable () -> Unit) {

        Column {

            Box(Modifier.fillMaxWidth()) {

                ListSelectionField(
                    items = NIFTY50,
                    onSelection = { symbol = it },
                    selection = symbol,
                )
            }

            chart()
        }
    }

    override fun CoroutineScope.configureChart(chart: IChartApi) {

        launch {

            var previous: ISeriesApi<*>? = null

            snapshotFlow { symbol }
                .distinctUntilChanged()
                .collect {

                    val dataTxt = Path("/home/saurabh/Downloads/Candles/ICICIBANK/M5/2022-10-01").readText()
                    val dataJson = appModule.json.parseToJsonElement(dataTxt)

                    val data = dataJson.jsonArray.map {

                        // Subtract IST Timezone difference
                        val epochTime = it.jsonArray[0].jsonPrimitive.long
                        val workaroundEpochTime = epochTime + 19800

                        CandlestickData(
                            time = workaroundEpochTime.toString(),
                            open = it.jsonArray[1].jsonPrimitive.content,
                            high = it.jsonArray[2].jsonPrimitive.content,
                            low = it.jsonArray[3].jsonPrimitive.content,
                            close = it.jsonArray[4].jsonPrimitive.content,
                        )
                    }

                    val dataVol = dataJson.jsonArray.map {

                        // Subtract IST Timezone difference
                        val epochTime = it.jsonArray[0].jsonPrimitive.long
                        val workaroundEpochTime = epochTime + 19800

                        HistogramData(
                            time = workaroundEpochTime.toString(),
                            value = it.jsonArray[5].jsonPrimitive.long,
                        )
                    }

                    previous?.let { chart.removeSeries(it) }
                    previous = chart.addCandlestickSeries()
                    val histogram = chart.addHistogramSeries()

                    @Suppress("UNCHECKED_CAST")
                    previous!!.setData(data as List<Nothing>)
                    histogram.setData(dataVol)

                    chart.timeScale.fitContent()
                    chart.timeScale.applyOptions(
                        TimeScaleOptions(timeVisible = true)
                    )
                }
        }
    }
}
