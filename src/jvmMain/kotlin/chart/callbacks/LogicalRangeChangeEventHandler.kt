package chart.callbacks

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun interface LogicalRangeChangeEventHandler {

    fun onEvent(range: LogicalRange?)
}

data class LogicalRange(
    val from: Float,
    val to: Float,
) {

    companion object {

        fun fromJson(jsonStr: String): LogicalRange? {

            val rangeElement = Json.parseToJsonElement(jsonStr)

            val from = rangeElement.jsonObject["from"]?.jsonPrimitive?.float ?: return null
            val to = rangeElement.jsonObject["to"]?.jsonPrimitive?.float ?: return null

            return LogicalRange(from = from, to = to)
        }
    }
}
