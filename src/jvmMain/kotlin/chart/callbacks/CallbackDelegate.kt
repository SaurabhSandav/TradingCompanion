package chart.callbacks

import chart.ISeriesApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class CallbackDelegate(
    private val chartName: String,
    private val seriesList: List<ISeriesApi<*>>,
) {

    // Chart
    internal val subscribeClickCallbacks = mutableListOf<MouseEventHandler>()
    internal val subscribeCrosshairMoveCallbacks = mutableListOf<MouseEventHandler>()

    // Timescale
    internal val subscribeVisibleTimeRangeChangeCallbacks = mutableListOf<TimeRangeChangeEventHandler>()
    internal val subscribeVisibleLogicalRangeChangeCallbacks = mutableListOf<LogicalRangeChangeEventHandler>()
    internal val subscribeSizeChangeCallbacks = mutableListOf<SizeChangeEventHandler>()

    fun onCallback(callbackStr: String) {

        val callbackElement = Json.parseToJsonElement(callbackStr)

        val chartCallback = ChartCallback(
            chartName = callbackElement.jsonObject["chartName"]!!.jsonPrimitive.content,
            callbackType = callbackElement.jsonObject["callbackType"]!!.jsonPrimitive.content,
            message = callbackElement.jsonObject["message"]!!.toString(),
        )

        // Callback not related to this chart
        if (chartCallback.chartName != chartName) return

        when (chartCallback.callbackType) {
            "subscribeClickCallback" -> {

                val params = MouseEventParams.fromJson(chartCallback.message, seriesList)

                subscribeClickCallbacks.forEach { it.onEvent(params) }
            }

            "subscribeCrosshairMoveCallback" -> {

                val params = MouseEventParams.fromJson(chartCallback.message, seriesList)

                subscribeCrosshairMoveCallbacks.forEach { it.onEvent(params) }
            }

            "subscribeVisibleTimeRangeChangeCallback" -> {

                val range = TimeRange.fromJson(chartCallback.message)

                subscribeVisibleTimeRangeChangeCallbacks.forEach { it.onEvent(range) }
            }

            "subscribeVisibleLogicalRangeChangeCallback" -> {

                val range = LogicalRange.fromJson(chartCallback.message)

                subscribeVisibleLogicalRangeChangeCallbacks.forEach { it.onEvent(range) }
            }

            "subscribeSizeChangeCallback" -> {

                val result = Json.parseToJsonElement(chartCallback.message)
                val width = result.jsonObject["width"]!!.jsonPrimitive.float
                val height = result.jsonObject["height"]!!.jsonPrimitive.float

                subscribeSizeChangeCallbacks.forEach { it.onEvent(width, height) }
            }

            else -> error("Unknown callback type. Callback: $chartCallback")
        }
    }
}
