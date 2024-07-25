package com.saurabhsandav.core.chart.data

import androidx.compose.ui.graphics.Color
import com.saurabhsandav.core.ui.common.toHexString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LineData(
    time: Time,
    value: Number,
    val color: Color? = null,
) : SingleValueData(time, value) {

    override fun toJsonElement(): JsonElement = buildJsonObject {

        putSingleValueDataElements(this@LineData)

        color?.let { put("color", it.toHexString()) }
    }
}
