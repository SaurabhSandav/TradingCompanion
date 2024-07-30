package com.saurabhsandav.lightweight_charts.data

import kotlinx.css.Color
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class HistogramData(
    time: Time,
    value: Number,
    val color: Color? = null,
) : SingleValueData(time, value) {

    override fun toJsonElement(): JsonElement = buildJsonObject {

        putSingleValueDataElements(this@HistogramData)

        color?.let { put("color", it.value) }
    }
}
