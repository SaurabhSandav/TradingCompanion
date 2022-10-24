package chart.options

import chart.IsJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TimeScaleOptions(
    private val timeVisible: Boolean? = null,
) : IsJsonElement {

    override fun toJsonElement(): JsonObject = buildJsonObject {
        timeVisible?.let { put("timeVisible", it) }
    }
}
