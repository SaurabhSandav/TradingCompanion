package chart.baseline

import chart.ISeriesApi
import javafx.application.Platform
import javafx.scene.web.WebEngine

internal class BaselineSeries(
    private val engine: WebEngine,
    override val name: String,
): ISeriesApi<BaselineData> {

    override fun setData(list: List<BaselineData>) {

        val dataJson = list.toJson()

        Platform.runLater {
            engine.executeScript("$name.setData($dataJson);")
        }
    }

    private fun List<BaselineData>.toJson(): String = buildString {

        append("[")

        this@toJson.forEach {
            append("{ time: \"${it.time}\", value: ${it.value} },")
        }

        append("]")
    }
}
