package com.saurabhsandav.core.chart

import com.saurabhsandav.core.chart.callbacks.CallbackDelegate
import com.saurabhsandav.core.chart.callbacks.CommandCallback
import com.saurabhsandav.core.chart.callbacks.MouseEventHandler
import com.saurabhsandav.core.chart.data.*
import com.saurabhsandav.core.chart.options.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IChartApi internal constructor(
    container: String = "document.body",
    options: ChartOptions = ChartOptions(),
    val name: String = "chart",
) {

    private val _scripts = Channel<String>(Channel.UNLIMITED)

    private val chartInstanceReference = "charts.get(\"$name\")"
    private val seriesMapReference = "$chartInstanceReference.seriesMap"
    private val subscribeClickCallbackReference = "$chartInstanceReference.subscribeClickCallback"
    private val subscribeCrosshairMoveCallbackReference = "$chartInstanceReference.subscribeCrosshairMoveCallback"

    private val seriesList = mutableListOf<ISeriesApi<*>>()
    private val callbacksDelegate = CallbackDelegate(name, seriesList)
    private var nextCommandCallbackId = 0

    val scripts: Flow<String> = _scripts.consumeAsFlow()

    private val reference = "$chartInstanceReference.chart"
    val timeScale = ITimeScaleApi(
        receiver = reference,
        chartInstanceReference = chartInstanceReference,
        callbacksDelegate = callbacksDelegate,
        executeJs = ::executeJs,
        executeJsWithResult = ::executeJsWithResult,
    )

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

        // Destroy chart
        executeJs("$reference.remove();")

        // Remove from JS cache
        executeJs("charts.delete(\"$name\")")

        // Close scripts stream
        _scripts.close()
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

    fun priceScale(priceScaleId: String): IPriceScaleApi {
        return IPriceScaleApi(reference, ::executeJs, priceScaleId)
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
            executeJsWithResult = ::executeJsWithResult,
            name = name,
            seriesInstanceReference = "$seriesMapReference.get(\"$name\")",
        )

        val optionsJson = options.toJsonElement()

        seriesList.add(series)

        executeJs("$seriesMapReference.set(\"$name\", new SeriesInstance($reference.$funcName(${optionsJson})));")

        return series
    }

    private fun executeJs(script: String) {
        _scripts.trySend(script)
    }

    private suspend fun executeJsWithResult(command: String): String = suspendCoroutine { continuation ->

        val id = nextCommandCallbackId++

        val commandCallback = CommandCallback(
            id = id,
            onResult = continuation::resume,
        )

        callbacksDelegate.commandCallbacks += commandCallback

        executeJs(
            """
            |(function() {
            |  var result = $command;
            |  chartCallback(
            |    JSON.stringify(new ChartCallback(
            |      "$name",
            |      "commandCallback",
            |      { id: $id, result: result },
            |    ))
            |  );
            |})()
            """.trimMargin()
        )
    }
}
