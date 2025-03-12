package com.saurabhsandav.lightweight_charts.callbacks

import com.saurabhsandav.lightweight_charts.data.IRange
import com.saurabhsandav.lightweight_charts.data.LogicalRange
import com.saurabhsandav.lightweight_charts.data.MouseEventParams
import com.saurabhsandav.lightweight_charts.data.Time
import com.saurabhsandav.lightweight_charts.utils.LwcJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class CallbackDelegate(
    private val chartId: String,
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
            chartId = callbackElement.jsonObject["chartId"]!!.jsonPrimitive.content,
            callbackType = callbackElement.jsonObject["callbackType"]!!.jsonPrimitive.content,
            message = callbackElement.jsonObject["message"]!!.toString(),
        )

        // Callback not related to this chart
        if (chartCallback.chartId != chartId) return

        when (chartCallback.callbackType) {
            "subscribeClickCallback" -> {

                val params = LwcJson.decodeFromString<MouseEventParams>(chartCallback.message)

                subscribeClickCallbacks.forEach { it.onEvent(params) }
            }

            "subscribeCrosshairMoveCallback" -> {

                val params = LwcJson.decodeFromString<MouseEventParams>(chartCallback.message)

                subscribeCrosshairMoveCallbacks.forEach { it.onEvent(params) }
            }

            "subscribeVisibleTimeRangeChangeCallback" -> {

                val range = LwcJson.decodeFromString<IRange<Time>?>(chartCallback.message)

                subscribeVisibleTimeRangeChangeCallbacks.forEach { it.onEvent(range) }
            }

            "subscribeVisibleLogicalRangeChangeCallback" -> {

                val range = LwcJson.decodeFromString<LogicalRange?>(chartCallback.message)

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
