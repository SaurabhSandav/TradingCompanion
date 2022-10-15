package chart.timescale

import chart.Chart
import javafx.application.Platform
import javafx.scene.web.WebEngine

internal class TimeScale(
    private val chart: Chart,
    private val engine: WebEngine,
) {

    fun fitContent() {

        Platform.runLater {
            engine.executeScript("${chart.name}.timeScale().fitContent();")
        }
    }

    fun applyOptions(options: TimeScaleOptions) {

        Platform.runLater {
            engine.executeScript("${chart.name}.timeScale().applyOptions(${options.toJson()});")
        }
    }
}
