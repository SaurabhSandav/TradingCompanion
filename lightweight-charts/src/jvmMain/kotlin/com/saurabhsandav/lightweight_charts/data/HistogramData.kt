package com.saurabhsandav.lightweight_charts.data

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.lightweight_charts.toHexString
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

        color?.let { put("color", it.toHexString()) }
    }
}
