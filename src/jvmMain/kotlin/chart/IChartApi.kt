package chart

import chart.data.*
import chart.options.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class IChartApi internal constructor(
    options: ChartOptions = ChartOptions(),
    val name: String = "chart",
) {

    private val _scripts = MutableSharedFlow<String>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE,
    )
    private val seriesList = mutableListOf<ISeriesApi<*>>()

    val scripts: Flow<String>
        get() = _scripts

    val timeScale = ITimeScaleApi(this, ::executeJs)
    val priceScale = IPriceScaleApi(name, ::executeJs)

    init {

        val optionsJson = options.toJsonElement()

        executeJs("const $name = LightweightCharts.createChart(document.body, $optionsJson);")

        executeJs("const seriesMap = new Map();")
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
        executeJs("$name.resize($width, $height);")
    }

    fun removeSeries(series: ISeriesApi<*>) {

        seriesList.remove(series)

        executeJs("$name.removeSeries(${series.reference});")
    }

    private fun <T : SeriesData> addSeries(
        options: SeriesOptions,
        funcName: String,
        name: String,
    ): ISeriesApi<T> {

        val series = ISeriesApi<T>(
            executeJs = ::executeJs,
            name = name,
            reference = "seriesMap.get(\"$name\")",
        )

        val optionsJson = options.toJsonElement()

        seriesList.add(series)

        executeJs("seriesMap.set(\"${name}\", ${this@IChartApi.name}.$funcName(${optionsJson}));")

        return series
    }

    private fun executeJs(script: String) {
        _scripts.tryEmit(script)
    }
}
