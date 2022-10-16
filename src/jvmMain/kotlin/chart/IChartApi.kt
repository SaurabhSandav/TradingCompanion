package chart

import chart.series.ISeriesApi
import chart.series.baseline.BaselineSeries
import chart.series.baseline.BaselineStyleOptions
import chart.series.candlestick.CandlestickSeries
import chart.series.candlestick.CandlestickStyleOptions
import chart.series.histogram.HistogramSeries
import chart.series.histogram.HistogramStyleOptions
import chart.series.timescale.ITimeScaleApi
import chart.series.pricescale.IPriceScaleApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class IChartApi(
    private val executeJs: (String) -> Unit,
    val name: String = "chart",
) {

    init {

        executeJs(
            """
                |const $name = LightweightCharts.createChart(document.body, {
                |    width: window.innerWidth,
                |    height: window.innerHeight
                |});
            """.trimMargin()
        )
    }

    private val json = Json { prettyPrint = true }

    val timeScale = ITimeScaleApi(this, executeJs, json)
    val priceScale = IPriceScaleApi(name, executeJs, json)

    fun addBaselineSeries(
        options: BaselineStyleOptions = BaselineStyleOptions(),
        name: String = "baselineSeries",
    ): BaselineSeries {

        val series = BaselineSeries(executeJs, json, name)
        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${buildSeriesDeclaration(series)}.addBaselineSeries(${optionsStr});")

        return series
    }

    fun addCandlestickSeries(
        options: CandlestickStyleOptions = CandlestickStyleOptions(),
        name: String = "candlestickSeries",
    ): CandlestickSeries {

        val series = CandlestickSeries(executeJs, json, name)
        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${buildSeriesDeclaration(series)}.addCandlestickSeries(${optionsStr});")

        return series
    }

    fun addHistogramSeries(
        options: HistogramStyleOptions = HistogramStyleOptions(),
        name: String = "histogramSeries",
    ): HistogramSeries {

        val series = HistogramSeries(executeJs, json, name)
        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${buildSeriesDeclaration(series)}.addHistogramSeries(${optionsStr});")

        return series
    }

    fun resize(width: Int, height: Int) {
        executeJs("$name.resize($width, $height)")
    }

    fun removeSeries(series: ISeriesApi<*>) {
        executeJs("$name.removeSeries(${series.name});")
    }

    private fun buildSeriesDeclaration(series: ISeriesApi<*>): String {
        return "var ${series.name} = (typeof ${series.name} != \"undefined\") ? ${series.name} : ${this@IChartApi.name}"
    }
}
