package com.saurabhsandav.lightweight_charts.misc

import com.saurabhsandav.lightweight_charts.IsJsonElement
import kotlinx.serialization.json.*

data class LogicalRange(
    val from: Float,
    val to: Float,
) : IsJsonElement {

    override fun toJsonElement(): JsonElement = buildJsonObject {
        put("from", from)
        put("to", to)
    }

    companion object {

        fun fromJson(jsonStr: String): LogicalRange? {

            val rangeElement = Json.parseToJsonElement(jsonStr)

            if (rangeElement == JsonNull) return null

            val from = rangeElement.jsonObject["from"]?.jsonPrimitive?.float ?: return null
            val to = rangeElement.jsonObject["to"]?.jsonPrimitive?.float ?: return null

            return LogicalRange(from = from, to = to)
        }
    }
}
