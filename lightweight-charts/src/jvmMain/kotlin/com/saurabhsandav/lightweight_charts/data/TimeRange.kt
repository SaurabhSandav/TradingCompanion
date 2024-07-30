package com.saurabhsandav.lightweight_charts.data

import com.saurabhsandav.lightweight_charts.callbacks.timeFromCallbackJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject

data class TimeRange(
    val from: Time,
    val to: Time,
) {

    companion object {

        fun fromJson(jsonStr: String): TimeRange? {

            val rangeElement = Json.parseToJsonElement(jsonStr)

            if (rangeElement == JsonNull) return null

            val from = timeFromCallbackJson(rangeElement.jsonObject["from"]) ?: return null
            val to = timeFromCallbackJson(rangeElement.jsonObject["to"]) ?: return null

            return TimeRange(from = from, to = to)
        }
    }
}
