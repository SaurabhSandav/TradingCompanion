package chart

import chart.callbacks.JavaCallbacks
import chart.callbacks.MouseEventHandler
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
    internal val javaCallbacksObjectName = "javaCallbacks"
    internal val javaCallbacks = JavaCallbacks(seriesList)

    val scripts: Flow<String>
        get() = _scripts

    val timeScale = ITimeScaleApi(this, ::executeJs)
    val priceScale = IPriceScaleApi(name, ::executeJs)

    init {

        val optionsJson = options.toJsonElement()

        executeJs("const $name = LightweightCharts.createChart(document.body, $optionsJson);")

        executeJs("const seriesMap = new Map();")

        executeJs(
            """|
            |
            |function getByValue(map, searchValue) {
            |  for (let [key, value] of map) {
            |    if (value === searchValue)
            |      return key;
            |  }
            |}
            |
            |function replacerSeriesByName(key, value) {
            |
            |    if (key == 'seriesPrices' && value instanceof Map) {
            |
            |        var namedMap = new Map();
            |
            |        value.forEach(function (value, key) {
            |            namedMap.set(getByValue(seriesMap, key), value);
            |        });
            |
            |        return Array.from(namedMap.entries());
            |    } else {
            |        return value;
            |    }
            |}
        """.trimMargin()
        )

        declareSubscribeClickCallback()
        declareSubscribeCrosshairMoveCallback()
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

    fun subscribeClick(handler: MouseEventHandler) {

        if (javaCallbacks.subscribeClickCallbacks.isEmpty())
            executeJs("$name.subscribeClick(subscribeClickCallback);")

        javaCallbacks.subscribeClickCallbacks.add(handler)
    }

    fun unsubscribeClick(handler: MouseEventHandler) {

        javaCallbacks.subscribeClickCallbacks.remove(handler)

        if (javaCallbacks.subscribeClickCallbacks.isEmpty())
            executeJs("$name.unsubscribeClick(subscribeClickCallback);")
    }

    fun subscribeCrosshairMove(handler: MouseEventHandler) {

        if (javaCallbacks.subscribeCrosshairMoveCallbacks.isEmpty())
            executeJs("$name.subscribeCrosshairMove(subscribeCrosshairMoveCallback);")

        javaCallbacks.subscribeCrosshairMoveCallbacks.add(handler)
    }

    fun unsubscribeCrosshairMove(handler: MouseEventHandler) {

        javaCallbacks.subscribeCrosshairMoveCallbacks.remove(handler)

        if (javaCallbacks.subscribeCrosshairMoveCallbacks.isEmpty())
            executeJs("$name.unsubscribeCrosshairMove(subscribeCrosshairMoveCallback);")
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

    private fun declareSubscribeClickCallback() {

        executeJs(
            """|
            |const subscribeClickCallback = (function (params) {
            |  $javaCallbacksObjectName.subscribeClickMouseEventHandler(JSON.stringify(params, replacerSeriesByName));
            |})
        """.trimMargin()
        )
    }

    private fun declareSubscribeCrosshairMoveCallback() {

        executeJs(
            """|
            |const subscribeCrosshairMoveCallback = (function (params) {
            |  $javaCallbacksObjectName.subscribeCrosshairMoveMouseEventHandler(JSON.stringify(params, replacerSeriesByName));
            |})
        """.trimMargin()
        )
    }

    private fun executeJs(script: String) {
        _scripts.tryEmit(script)
    }
}
