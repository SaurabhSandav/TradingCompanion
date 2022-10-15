package chart

import chart.baseline.BaselineSeries
import chart.candlestick.CandlestickSeries
import chart.histogram.HistogramSeries
import chart.timescale.TimeScale
import javafx.application.Platform
import javafx.scene.web.WebEngine

internal class IChartApi(
    private val engine: WebEngine,
    val name: String = "chart",
) {

    init {

        Platform.runLater {
            engine.executeScript(
                """
                    const $name = LightweightCharts.createChart(document.body, {
                        width: window.innerWidth,
                        height: window.innerHeight
                    });
                """.trimIndent()
            )
        }
    }

    val timeScale = TimeScale(this, engine)

    fun addBaselineSeries(name: String = "baselineSeries"): BaselineSeries {

        val series = BaselineSeries(engine, name)

        Platform.runLater {
            engine.executeScript(
                """
                    const ${series.name} = ${this@IChartApi.name}.addBaselineSeries({
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

        return series
    }

    fun addCandlestickSeries(name: String = "candlestickSeries"): CandlestickSeries {

        val series = CandlestickSeries(engine, name)

        Platform.runLater {
            engine.executeScript(
                """
                    const ${series.name} = ${this@IChartApi.name}.addCandlestickSeries({
                        upColor: '#26a69a',
                        downColor: '#ef5350',
                        borderVisible: false,
                        wickUpColor: '#26a69a',
                        wickDownColor: '#ef5350'
                    });
                """.trimIndent()
            )
        }

        return series
    }

    fun addHistogramSeries(name: String = "histogramSeries"): HistogramSeries {

        val series = HistogramSeries(engine, name)

        Platform.runLater {
            engine.executeScript(
                """
                    const ${series.name} = ${this@IChartApi.name}.addHistogramSeries({
                        color: '#26a69a',
                        priceFormat: {
                            type: 'volume',
                        },
                        priceScaleId: '',
                        scaleMargins: {
                            top: 0.8,
                            bottom: 0,
                        },
                    });
                """.trimIndent()
            )
        }

        return series
    }

    fun resize(width: Int, height: Int) {
        Platform.runLater {
            engine.executeScript("$name.resize($width, $height)")
        }
    }

    fun removeSeries(series: ISeriesApi<*>) {
        Platform.runLater {
            engine.executeScript("$name.removeSeries(${series.name});")
        }
    }
}
