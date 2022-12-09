package chart.callbacks

import chart.data.Time
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

fun interface TimeRangeChangeEventHandler {

    fun onEvent(range: TimeRange?)
}

data class TimeRange(
    val from: Time,
    val to: Time,
) {

    companion object {

        fun fromJson(jsonStr: String): TimeRange? {

            val rangeElement = Json.parseToJsonElement(jsonStr)

            val from = timeFromCallbackJson(rangeElement.jsonObject["from"]) ?: return null
            val to = timeFromCallbackJson(rangeElement.jsonObject["to"]) ?: return null

            return TimeRange(from = from, to = to)
        }
    }
}
