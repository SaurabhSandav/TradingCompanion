package com.saurabhsandav.core.chart.misc

import com.saurabhsandav.core.chart.callbacks.timeFromCallbackJson
import com.saurabhsandav.core.chart.data.Time
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
