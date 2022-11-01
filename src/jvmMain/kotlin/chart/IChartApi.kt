package chart

import chart.data.*
import chart.options.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun createChart(
    options: ChartOptions = ChartOptions(),
    name: String = "chart",
): IChartApi = IChartApi(options, name)

class IChartApi internal constructor(
    options: ChartOptions = ChartOptions(),
    val name: String = "chart",
) {

    private val _scripts = MutableSharedFlow<String>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE,
    )
    val scripts: Flow<String>
        get() = _scripts

    val timeScale = ITimeScaleApi(this, ::executeJs)
    val priceScale = IPriceScaleApi(name, ::executeJs)

    init {

        val optionsJson = options.toJsonElement()

        executeJs("const $name = LightweightCharts.createChart(document.body, $optionsJson);")
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
        executeJs("$name.resize($width, $height)")
    }

    fun removeSeries(series: ISeriesApi<*>) {
        executeJs("$name.removeSeries(${series.name});")
    }

    private fun <T : SeriesData> addSeries(
        options: SeriesOptions,
        funcName: String,
        name: String,
    ): ISeriesApi<T> {

        val series = ISeriesApi<T>(::executeJs, name)
        val optionsJson = options.toJsonElement()

        executeJs("var ${series.name} = (typeof ${series.name} != \"undefined\") ? ${series.name} : ${this@IChartApi.name}.$funcName(${optionsJson});")

        return series
    }

    private fun executeJs(script: String) {
        _scripts.tryEmit(script)
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
