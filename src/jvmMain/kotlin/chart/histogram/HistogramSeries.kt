package chart.histogram

import chart.ISeriesApi
import javafx.application.Platform
import javafx.scene.web.WebEngine

internal class HistogramSeries(
    private val engine: WebEngine,
    override val name: String,
): ISeriesApi<HistogramData> {

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
