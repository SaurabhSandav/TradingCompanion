package chart.histogram

import chart.ChartSeries
import javafx.application.Platform
import javafx.scene.web.WebEngine

internal class HistogramSeries(
    private val engine: WebEngine,
    override val name: String,
): ChartSeries<HistogramData> {

    override fun setData(list: List<HistogramData>) {

        val dataJson = list.toJson()

        Platform.runLater {
            engine.executeScript("$name.setData($dataJson);")
        }
    }

    private fun List<HistogramData>.toJson(): String = buildString {

        append("[")

        this@toJson.forEach {
            append("{ time: ${it.time}, value: ${it.value} },")
        }

        append("]")
    }
}
