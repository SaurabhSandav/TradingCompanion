package chart

import chart.callbacks.CallbackDelegate
import chart.callbacks.MouseEventHandler
import chart.data.*
import chart.options.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class IChartApi internal constructor(
    container: String = "document.body",
    options: ChartOptions = ChartOptions(),
    val name: String = "chart",
) {

    private val _scripts = MutableSharedFlow<String>(
        replay = Int.MAX_VALUE,
        extraBufferCapacity = Int.MAX_VALUE,
    )

    private val chartInstanceReference = "charts.get(\"$name\")"
    private val seriesMapReference = "$chartInstanceReference.seriesMap"
    private val subscribeClickCallbackReference = "$chartInstanceReference.subscribeClickCallback"
    private val subscribeCrosshairMoveCallbackReference = "$chartInstanceReference.subscribeCrosshairMoveCallback"

    private val seriesList = mutableListOf<ISeriesApi<*>>()
    private val callbacksDelegate = CallbackDelegate(name, seriesList)

    val scripts: Flow<String>
        get() = _scripts

    val reference = "$chartInstanceReference.chart"
    val timeScale = ITimeScaleApi(this, ::executeJs)
    val priceScale = IPriceScaleApi(reference, ::executeJs)

    init {

        val optionsJson = options.toJsonElement()

        executeJs(
            """
            |charts.set("$name", new ChartInstance(
            |  "$name",
            |  LightweightCharts.createChart($container, $optionsJson),
            |));""".trimMargin()
        )
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

    fun remove() {
        executeJs("$reference.remove();")
    }

    fun resize(width: Int, height: Int) {
        executeJs("$reference.resize($width, $height);")
    }

    fun removeSeries(series: ISeriesApi<*>) {

        seriesList.remove(series)

        executeJs("$reference.removeSeries(${series.reference});")

        executeJs("$seriesMapReference.delete(\"$name\");")
    }

    fun subscribeClick(handler: MouseEventHandler) {

        if (callbacksDelegate.subscribeClickCallbacks.isEmpty())
            executeJs("$reference.subscribeClick($subscribeClickCallbackReference);")

        callbacksDelegate.subscribeClickCallbacks.add(handler)
    }

    fun unsubscribeClick(handler: MouseEventHandler) {

        callbacksDelegate.subscribeClickCallbacks.remove(handler)

        if (callbacksDelegate.subscribeClickCallbacks.isEmpty())
            executeJs("$reference.unsubscribeClick($subscribeClickCallbackReference);")
    }

    fun subscribeCrosshairMove(handler: MouseEventHandler) {

        if (callbacksDelegate.subscribeCrosshairMoveCallbacks.isEmpty())
            executeJs("$reference.subscribeCrosshairMove($subscribeCrosshairMoveCallbackReference);")

        callbacksDelegate.subscribeCrosshairMoveCallbacks.add(handler)
    }

    fun unsubscribeCrosshairMove(handler: MouseEventHandler) {

        callbacksDelegate.subscribeCrosshairMoveCallbacks.remove(handler)

        if (callbacksDelegate.subscribeCrosshairMoveCallbacks.isEmpty())
            executeJs("$reference.unsubscribeCrosshairMove($subscribeCrosshairMoveCallbackReference);")
    }

    fun applyOptions(options: ChartOptions) {

        val optionsJson = options.toJsonElement()

        executeJs("$reference.applyOptions($optionsJson);")
    }

    fun onCallback(callbackMessage: String) {
        callbacksDelegate.onCallback(callbackMessage)
    }

    private fun <T : SeriesData> addSeries(
        options: SeriesOptions,
        funcName: String,
        name: String,
    ): ISeriesApi<T> {

        val series = ISeriesApi<T>(
            executeJs = ::executeJs,
            name = name,
            seriesInstanceReference = "$seriesMapReference.get(\"$name\")",
        )

        val optionsJson = options.toJsonElement()

        seriesList.add(series)

        executeJs("$seriesMapReference.set(\"$name\", new SeriesInstance($reference.$funcName(${optionsJson})));")

        return series
    }

    private fun executeJs(script: String) {
        _scripts.tryEmit(script)
    }
}
