package com.saurabhsandav.core.chart.options

import com.saurabhsandav.core.chart.IsJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TimeScaleOptions(
    private val timeVisible: Boolean? = null,
    private val secondsVisible: Boolean? = null,
    private val shiftVisibleRangeOnNewBar: Boolean? = null,
) : IsJsonElement {

    override fun toJsonElement(): JsonObject = buildJsonObject {
        timeVisible?.let { put("timeVisible", it) }
        secondsVisible?.let { put("secondsVisible", it) }
        shiftVisibleRangeOnNewBar?.let { put("shiftVisibleRangeOnNewBar", it) }
    }
}
