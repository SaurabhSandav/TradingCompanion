package chart

import chart.pricescale.IPriceScaleApi
import chart.series.ISeriesApi
import chart.series.baseline.BaselineSeries
import chart.series.baseline.BaselineStyleOptions
import chart.series.candlestick.CandlestickSeries
import chart.series.candlestick.CandlestickStyleOptions
import chart.series.histogram.HistogramSeries
import chart.series.histogram.HistogramStyleOptions
import chart.series.line.LineSeries
import chart.series.line.LineStyleOptions
import chart.timescale.ITimeScaleApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.properties.ReadOnlyProperty

fun createChart(
    options: ChartOptions = ChartOptions(),
    name: String = "chart",
): IChartApi = IChartApi(options, name)

class IChartApi internal constructor(
    private val options: ChartOptions = ChartOptions(),
    val name: String = "chart",
) {

    private val json = Json { prettyPrint = true }
    private lateinit var executeJs: (String) -> Unit

    var isInitialized = false
    lateinit var timeScale: ITimeScaleApi
    lateinit var priceScale: IPriceScaleApi

    fun init(
        container: String,
        executeJs: (String) -> Unit,
    ) {

        this.executeJs = executeJs
        timeScale = ITimeScaleApi(this, executeJs, json)
        priceScale = IPriceScaleApi(name, executeJs, json)

        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("const $name = LightweightCharts.createChart($container, $optionsStr);")

        isInitialized = true
    }

    fun addBaselineSeries(
        options: BaselineStyleOptions = BaselineStyleOptions(),
        name: String = "baselineSeries",
    ): BaselineSeries {

        checkChartInitialized()

        val series = BaselineSeries(executeJs, json, name)
        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${buildSeriesDeclaration(series)}.addBaselineSeries(${optionsStr});")

        return series
    }

    fun addCandlestickSeries(
        options: CandlestickStyleOptions = CandlestickStyleOptions(),
        name: String = "candlestickSeries",
    ): CandlestickSeries {

        checkChartInitialized()

        val series = CandlestickSeries(executeJs, json, name)
        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${buildSeriesDeclaration(series)}.addCandlestickSeries(${optionsStr});")

        return series
    }

    fun addHistogramSeries(
        options: HistogramStyleOptions = HistogramStyleOptions(),
        name: String = "histogramSeries",
    ): HistogramSeries {

        checkChartInitialized()

        val series = HistogramSeries(executeJs, json, name)
        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${buildSeriesDeclaration(series)}.addHistogramSeries(${optionsStr});")

        return series
    }

    fun addLineSeries(
        options: LineStyleOptions = LineStyleOptions(),
        name: String = "lineSeries",
    ): LineSeries {

        checkChartInitialized()

        val series = LineSeries(executeJs, json, name)
        val optionsStr = json.encodeToString(options.toJsonObject())

        executeJs("${buildSeriesDeclaration(series)}.addLineSeries(${optionsStr});")

        return series
    }

    fun resize(width: Int, height: Int) {

        checkChartInitialized()

        executeJs("$name.resize($width, $height)")
    }

    fun removeSeries(series: ISeriesApi<*>) {

        checkChartInitialized()

        executeJs("$name.removeSeries(${series.name});")
    }

    private fun buildSeriesDeclaration(series: ISeriesApi<*>): String {
        return "var ${series.name} = (typeof ${series.name} != \"undefined\") ? ${series.name} : ${this@IChartApi.name}"
    }

    private fun checkChartInitialized() {
        check(isInitialized) { "Chart is not initialized. Call init() before any chart operations." }
    }
}

fun IChartApi.baselineSeries(
    options: BaselineStyleOptions = BaselineStyleOptions(),
) = ReadOnlyProperty<Any?, BaselineSeries> { _, property -> addBaselineSeries(options, property.name) }

fun IChartApi.candlestickSeries(
    options: CandlestickStyleOptions = CandlestickStyleOptions(),
) = ReadOnlyProperty<Any?, CandlestickSeries> { _, property -> addCandlestickSeries(options, property.name) }

fun IChartApi.histogramSeries(
    options: HistogramStyleOptions = HistogramStyleOptions(),
) = ReadOnlyProperty<Any?, HistogramSeries> { _, property -> addHistogramSeries(options, property.name) }

fun IChartApi.lineSeries(
    options: LineStyleOptions = LineStyleOptions(),
) = ReadOnlyProperty<Any?, LineSeries> { _, property -> addLineSeries(options, property.name) }
