package chart.baseline

import chart.Chart
import javafx.application.Platform
import javafx.scene.web.WebEngine

internal class BaselineSeries(
    private val chart: Chart,
    private val engine: WebEngine,
) {

    fun setData(list: List<BaselineData>) {

        val dataJson = list.toJson()

        Platform.runLater {
            engine.executeScript(
                """
                    baselineSeries.setData($dataJson);

                    chart.timeScale().fitContent();
                """.trimIndent()
            )
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
