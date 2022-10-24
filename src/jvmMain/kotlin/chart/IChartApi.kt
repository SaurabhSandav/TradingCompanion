package chart

import chart.data.*
import chart.options.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun createChart(
    options: ChartOptions = ChartOptions(),
    name: String = "chart",
): IChartApi = IChartApi(options, name)

class IChartApi internal constructor(
    private val options: ChartOptions = ChartOptions(),
    val name: String = "chart",
) {

    private lateinit var executeJs: (String) -> Unit

    var isInitialized = false
    lateinit var timeScale: ITimeScaleApi
    lateinit var priceScale: IPriceScaleApi

    fun init(
        container: String,
        executeJs: (String) -> Unit,
    ) {

        this.executeJs = executeJs
        timeScale = ITimeScaleApi(this, executeJs)
        priceScale = IPriceScaleApi(name, executeJs)

        val optionsJson = options.toJsonElement()

        executeJs("const $name = LightweightCharts.createChart($container, $optionsJson);")

        isInitialized = true
    }

    fun addBaselineSeries(
        options: BaselineStyleOptions = BaselineStyleOptions(),
        name: String = "baselineSeries",
    ): ISeriesApi<SingleValueData> = addSeries(
        options = options,
        funcName = "addBaselineSeries",
        name = name
    )

    fun addCandlestickSeries(
        options: CandlestickStyleOptions = CandlestickStyleOptions(),
        name: String = "candlestickSeries",
    ): ISeriesApi<CandlestickData> = addSeries(
        options = options,
        funcName = "addCandlestickSeries",
        name = name
    )

    fun addHistogramSeries(
        options: HistogramStyleOptions = HistogramStyleOptions(),
        name: String = "histogramSeries",
    ): ISeriesApi<HistogramData> = addSeries(
        options = options,
        funcName = "addHistogramSeries",
        name = name
    )

    fun addLineSeries(
        options: LineStyleOptions = LineStyleOptions(),
        name: String = "lineSeries",
    ): ISeriesApi<LineData> = addSeries(
        options = options,
        funcName = "addLineSeries",
        name = name
    )

    fun resize(width: Int, height: Int) {

        checkChartInitialized()

        executeJs("$name.resize($width, $height)")
    }

    fun removeSeries(series: ISeriesApi<*>) {

        checkChartInitialized()

        executeJs("$name.removeSeries(${series.name});")
    }

    private fun <T : SeriesData> addSeries(
        options: SeriesOptions,
        funcName: String,
        name: String,
    ): ISeriesApi<T> {

        checkChartInitialized()

        val series = ISeriesApi<T>(executeJs, name)
        val optionsJson = options.toJsonElement()

        executeJs("var ${series.name} = (typeof ${series.name} != \"undefined\") ? ${series.name} : ${this@IChartApi.name}.$funcName(${optionsJson});")

        return series
    }

    private fun checkChartInitialized() {
        check(isInitialized) { "Chart is not initialized. Call init() before any chart operations." }
    }
}

fun IChartApi.baselineSeries(
    options: BaselineStyleOptions = BaselineStyleOptions(),
) = SeriesProvider { name -> addBaselineSeries(options, name) }

fun IChartApi.candlestickSeries(
    options: CandlestickStyleOptions = CandlestickStyleOptions(),
) = SeriesProvider { name -> addCandlestickSeries(options, name) }

fun IChartApi.histogramSeries(
    options: HistogramStyleOptions = HistogramStyleOptions(),
) = SeriesProvider { name -> addHistogramSeries(options, name) }

fun IChartApi.lineSeries(
    options: LineStyleOptions = LineStyleOptions(),
) = SeriesProvider { name -> addLineSeries(options, name) }

class SeriesProvider<T : SeriesData>(
    private val seriesBuilder: (propertyName: String) -> ISeriesApi<T>,
) {

    var series: ISeriesApi<T>? = null

    operator fun provideDelegate(
        thisRef: Any?,
        prop: KProperty<*>,
    ): ReadOnlyProperty<Any?, ISeriesApi<T>> {

        series = seriesBuilder(prop.name)

        return ReadOnlyProperty { _, _ -> series!! }
    }
}
