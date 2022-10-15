package chart.candlestick

import chart.ChartSeries
import javafx.application.Platform
import javafx.scene.web.WebEngine

internal class CandlestickSeries(
    private val engine: WebEngine,
    override val name: String,
): ChartSeries<CandlestickData> {

    override fun setData(list: List<CandlestickData>) {

        val dataJson = list.toJson()

        Platform.runLater {
            engine.executeScript("$name.setData($dataJson);")
        }
    }

    private fun List<CandlestickData>.toJson(): String = buildString {

        append("[")

        this@toJson.forEach {
            append("{ time: ${it.time}, open: ${it.open}, high: ${it.high}, low: ${it.low}, close: ${it.close} },")
        }

        append("]")
    }
}
