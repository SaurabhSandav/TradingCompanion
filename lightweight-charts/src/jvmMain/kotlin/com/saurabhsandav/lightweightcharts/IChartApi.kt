package com.saurabhsandav.lightweightcharts

import com.saurabhsandav.lightweightcharts.callbacks.CallbackDelegate
import com.saurabhsandav.lightweightcharts.callbacks.CommandCallback
import com.saurabhsandav.lightweightcharts.callbacks.MouseEventHandler
import com.saurabhsandav.lightweightcharts.data.SeriesData
import com.saurabhsandav.lightweightcharts.data.Time
import com.saurabhsandav.lightweightcharts.options.ChartOptions
import com.saurabhsandav.lightweightcharts.options.SeriesOptions
import com.saurabhsandav.lightweightcharts.utils.LwcJson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.uuid.Uuid

class IChartApi internal constructor(
    container: String = "document.body",
    options: ChartOptions? = null,
    val id: String = Uuid.random().toString(),
) {

    private val _scripts = Channel<String>(Channel.UNLIMITED)

    private val chartInstanceReference = "charts.get(\"$id\")"
    private val seriesMapReference = "$chartInstanceReference.seriesMap"
    private val panesMapReference = "$chartInstanceReference.panesMap"
    private val subscribeClickCallbackReference = "$chartInstanceReference.subscribeClickCallback"
    private val subscribeCrosshairMoveCallbackReference = "$chartInstanceReference.subscribeCrosshairMoveCallback"

    private val seriesList = mutableListOf<ISeriesApi<*, *>>()
    internal val panesList = mutableListOf<IPaneApi>()
    private val callbacksDelegate = CallbackDelegate(id)
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

        val optionsJson = LwcJson.encodeToString(options)

        executeJs(
            """
            |charts.set("$id", new ChartInstance(
            |  "$id",
            |  LightweightCharts.createChart($container, $optionsJson),
            |  $ChartCallbackFunc,
            |));
            """.trimMargin(),
        )

        if (options?.addDefaultPane == null || options.addDefaultPane) {
            createIPaneApi("$reference.panes()[0]")
            panesList.single().setPreserveEmptyPane(true)
        }
    }

    private fun createIPaneApi(initializer: String): IPaneApi {

        val paneId = Uuid.random().toString()

        executeJs("$panesMapReference.set(\"$paneId\", $initializer);")

        val pane = IPaneApi(
            iChartApi = this,
            executeJs = ::executeJs,
            executeJsWithResult = ::executeJsWithResult,
            id = paneId,
            paneReference = "$panesMapReference.get(\"$paneId\")",
        )

        panesList += pane

        return pane
    }

    fun remove() {

        // Destroy chart
        executeJs("$reference.remove();")

        // Remove from JS cache
        executeJs("charts.delete(\"$id\")")

        // Close scripts stream
        _scripts.close()
    }

    fun resize(
        width: Int,
        height: Int,
        forceRepaint: Boolean?,
    ) {

        val arguments = buildString {
            append(width)
            append(", ")
            append(height)
            forceRepaint?.let {
                append(", ")
                append(it)
            }
        }

        executeJs("$reference.resize($arguments);")
    }

    fun <D : SeriesData, O : SeriesOptions> addSeries(
        definition: SeriesDefinition<D, O>,
        options: O? = null,
        paneIndex: Int? = null,
    ): ISeriesApi<D, O> {

        val seriesId = Uuid.random().toString()
        val series = ISeriesApi(
            definition = definition,
            executeJs = ::executeJs,
            executeJsWithResult = ::executeJsWithResult,
            id = seriesId,
            seriesInstanceReference = "$seriesMapReference.get(\"$seriesId\")",
            paneIndex = paneIndex ?: 0,
            getPanesList = { panesList },
        )

        seriesList.add(series)

        val seriesArguments = buildString {
            append(definition.jsStatement)
            options?.let {
                append(", ")
                append(LwcJson.encodeToString(definition.optionsSerializer, it))
            }
            paneIndex?.let {
                append(", ")
                append(it)
            }
        }

        executeJs(
            """
            |$seriesMapReference.set("$seriesId", new SeriesInstance(
            |  $reference.addSeries($seriesArguments)
            |));
            """.trimMargin(),
        )

        return series
    }

    fun removeSeries(series: ISeriesApi<*, *>) {

        seriesList.remove(series)

        executeJs("$reference.removeSeries(${series.reference});")

        executeJs("$seriesMapReference.delete(\"${series.id}\");")
    }

    fun subscribeClick(handler: MouseEventHandler) {

        if (callbacksDelegate.subscribeClickCallbacks.isEmpty()) {
            executeJs("$reference.subscribeClick($subscribeClickCallbackReference);")
        }

        callbacksDelegate.subscribeClickCallbacks.add(handler)
    }

    fun unsubscribeClick(handler: MouseEventHandler) {

        callbacksDelegate.subscribeClickCallbacks.remove(handler)

        if (callbacksDelegate.subscribeClickCallbacks.isEmpty()) {
            executeJs("$reference.unsubscribeClick($subscribeClickCallbackReference);")
        }
    }

    fun subscribeCrosshairMove(handler: MouseEventHandler) {

        if (callbacksDelegate.subscribeCrosshairMoveCallbacks.isEmpty()) {
            executeJs("$reference.subscribeCrosshairMove($subscribeCrosshairMoveCallbackReference);")
        }

        callbacksDelegate.subscribeCrosshairMoveCallbacks.add(handler)
    }

    fun unsubscribeCrosshairMove(handler: MouseEventHandler) {

        callbacksDelegate.subscribeCrosshairMoveCallbacks.remove(handler)

        if (callbacksDelegate.subscribeCrosshairMoveCallbacks.isEmpty()) {
            executeJs("$reference.unsubscribeCrosshairMove($subscribeCrosshairMoveCallbackReference);")
        }
    }

    fun priceScale(priceScaleId: String): IPriceScaleApi {
        return IPriceScaleApi(
            receiver = reference,
            executeJs = ::executeJs,
            executeJsWithResult = ::executeJsWithResult,
            priceScaleId = priceScaleId,
        )
    }

    fun setCrosshairPosition(
        price: Double,
        horizontalPosition: Time,
        seriesApi: ISeriesApi<*, *>,
    ) {

        val horizontalPositionJson = LwcJson.encodeToString(horizontalPosition)

        executeJs("$reference.setCrosshairPosition($price, $horizontalPositionJson, ${seriesApi.reference});")
    }

    fun clearCrosshairPosition() {
        executeJs("$reference.clearCrosshairPosition();")
    }

    fun addPane(): IPaneApi {
        return createIPaneApi("$reference.addPane(true)")
    }

    fun panes(): List<IPaneApi> = panesList

    fun removePane(index: Int) {
        val pane = panesList.removeAt(index)
        executeJs("$panesMapReference.delete(\"${pane.id}\");")
        executeJs("$reference.removePane($index);")
    }

    fun swapPanes(
        first: Int,
        second: Int,
    ) {

        require(first !in 0..<panesList.size || second !in 0..<panesList.size) {
            "Indexes must be within the list bounds."
        }

        val temp = panesList[first]
        panesList[first] = panesList[second]
        panesList[second] = temp

        executeJs("$reference.swapPanes($first, $second);")
    }

    fun applyOptions(options: ChartOptions) {

        val optionsJson = LwcJson.encodeToString(options)

        executeJs("$reference.applyOptions($optionsJson);")
    }

    fun onCallback(callbackMessage: String) {
        callbacksDelegate.onCallback(callbackMessage)
    }

    private fun executeJs(script: String) {
        _scripts.trySend(script)
    }

    private suspend fun executeJsWithResult(command: String): String = suspendCancellableCoroutine { continuation ->

        val chartId = id
        val id = nextCommandCallbackId++

        val commandCallback = CommandCallback(
            id = id,
            onResult = continuation::resume,
        )

        callbacksDelegate.commandCallbacks += commandCallback

        continuation.invokeOnCancellation {
            callbacksDelegate.commandCallbacks -= commandCallback
        }

        executeJs(
            """
            |(function() {
            |  var result = $command;
            |  $ChartCallbackFunc(
            |    JSON.stringify(new ChartCallback(
            |      "$chartId",
            |      "commandCallback",
            |      { id: $id, result: result },
            |    ))
            |  );
            |})()
            """.trimMargin(),
        )
    }
}

const val ChartCallbackFunc = "chartCallback"
