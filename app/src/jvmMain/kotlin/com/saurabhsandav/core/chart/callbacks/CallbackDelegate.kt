package com.saurabhsandav.core.chart.callbacks

import com.saurabhsandav.core.chart.ISeriesApi
import com.saurabhsandav.core.chart.misc.LogicalRange
import com.saurabhsandav.core.chart.misc.MouseEventParams
import com.saurabhsandav.core.chart.misc.TimeRange
import kotlinx.serialization.json.*

internal class CallbackDelegate(
    private val chartName: String,
    private val seriesList: List<ISeriesApi<*>>,
) {

    // Commands
    internal val commandCallbacks = mutableListOf<CommandCallback>()

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

            "commandCallback" -> {

                val message = Json.parseToJsonElement(chartCallback.message)
                val id = message.jsonObject["id"]!!.jsonPrimitive.int
                val result = message.jsonObject["result"]!!.toString()

                val callback = commandCallbacks.find { it.id == id }!!
                commandCallbacks -= callback

                callback.onResult(result)
            }

            else -> error("Unknown callback type. Callback: $chartCallback")
        }
    }
}
