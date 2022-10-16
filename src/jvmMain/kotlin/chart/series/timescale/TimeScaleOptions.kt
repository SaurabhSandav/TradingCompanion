package chart.series.timescale

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TimeScaleOptions(
    private val timeVisible: Boolean? = null,
) {

    fun toJsonObject(): JsonObject = buildJsonObject {
        timeVisible?.let { put("timeVisible", it) }
    }
}
