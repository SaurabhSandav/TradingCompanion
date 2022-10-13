package chart

import chart.baseline.BaselineSeries
import javafx.application.Platform
import javafx.scene.web.WebEngine

internal class Chart(
    private val engine: WebEngine,
) {

    init {

        Platform.runLater {
            engine.executeScript(
                """
                    const chart = LightweightCharts.createChart(document.body, {
                        width: window.innerWidth,
                        height: window.innerHeight
                    });
                """.trimIndent()
            )
        }
    }

    fun addBaselineSeries(): BaselineSeries {

        Platform.runLater {
            engine.executeScript(
                """
                    const baselineSeries = chart.addBaselineSeries({
                        baseValue: {
                            type: 'price',
                            price: 0
                        },
                        topLineColor: 'rgba( 38, 166, 154, 1)',
                        topFillColor1: 'rgba( 38, 166, 154, 0.28)',
                        topFillColor2: 'rgba( 38, 166, 154, 0.05)',
                        bottomLineColor: 'rgba( 239, 83, 80, 1)',
                        bottomFillColor1: 'rgba( 239, 83, 80, 0.05)',
                        bottomFillColor2: 'rgba( 239, 83, 80, 0.28)'
                    });
                """.trimIndent()
            )
        }

        return BaselineSeries(this, engine)
    }

    fun resize(width: Int, height: Int) {
        Platform.runLater {
            engine.executeScript("chart.resize($width, $height)")
        }
    }
}
